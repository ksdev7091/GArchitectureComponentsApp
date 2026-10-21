# 解答例(01-new-app)

解答は「抜粋」です。自分の実装と比べて、違いの理由を考えてください。

## 1 並び順の切り替え

**UseCase に引数を足す**案を採ります(「検索して並べる」は 1 つの業務操作で、別 UseCase にすると
呼び出し側が 2 つを組み合わせる責任を負う)。

```kotlin
// domain/GetNotesUseCase.kt
enum class SortOrder { UPDATED_DESC, TITLE_ASC }

class GetNotesUseCase @Inject constructor(private val notesRepository: NotesRepository) {
    operator fun invoke(query: String = "", sortOrder: SortOrder = SortOrder.UPDATED_DESC): Flow<List<Note>> {
        val normalized = query.trim()
        return notesRepository.observeNotes().map { notes ->
            val filtered = if (normalized.isEmpty()) {
                notes
            } else {
                notes.filter { note ->
                    note.title.contains(normalized, ignoreCase = true) ||
                        note.body.contains(normalized, ignoreCase = true)
                }
            }
            when (sortOrder) {
                // 更新順は Repository(DAO の SQL)がすでに保証している。ここで並べ直すと
                // 「並び順は DAO が決める」という既存のテストの前提(Fake は渡した順を返す)が崩れる
                SortOrder.UPDATED_DESC -> filtered
                SortOrder.TITLE_ASC -> filtered.sortedBy { it.title }
            }
        }
    }
}
```

`UPDATED_DESC` で並べ直さないのは手抜きではありません。並び順の責任を 2 か所(DAO と UseCase)に持たせると、
どちらが正か分からなくなります。「既定の並びは DAO、それ以外の並びは UseCase」と責任を分けています。

```kotlin
// ui/notes/NotesViewModel.kt
private val sortOrder = MutableStateFlow(SortOrder.UPDATED_DESC)

private val notes = combine(query, sortOrder) { q, s -> q to s }
    .flatMapLatest { (q, s) -> getNotes(q, s) }

val uiState = combine(notes, query, sortOrder, isRefreshing, userMessageRes) { notes, q, s, refreshing, msg ->
    NotesUiState(notes = notes, query = q, sortOrder = s, isLoading = false, isRefreshing = refreshing, userMessageRes = msg)
}.stateIn(...)

fun toggleSortOrder() {
    sortOrder.update { if (it == SortOrder.UPDATED_DESC) SortOrder.TITLE_ASC else SortOrder.UPDATED_DESC }
}
```

`combine` は 5 引数まで型付きのオーバーロードがあります。6 つ以上になったら、関連する入力をまとめた
data class を先に `combine` するか、UiState の設計を見直す合図です。

```kotlin
// NotesViewModelTest.kt
@Test
fun `並び順を切り替えるとタイトル順になる`() = runTest {
    val repository = FakeNotesRepository(listOf(note(1, "b"), note(2, "a")))
    val viewModel = createViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    viewModel.toggleSortOrder()

    assertEquals(SortOrder.TITLE_ASC, viewModel.uiState.value.sortOrder)
    assertEquals(listOf("a", "b"), viewModel.uiState.value.notes.map { it.title })
}
```

**DAO で並べる案との比較**: SQL なら大量データでも軽いが、並び順ごとに `@Query` と Repository のメソッドが増え、
Fake にも並び替えを実装する必要が出る。この規模ではメモリ上で並べるのが単純。
ページングする規模になったら SQL に移す(02 の演習 3-2 と同じ判断基準)。

回転で戻らないのは、`sortOrder` が ViewModel にあるから。プロセス再生成でも残したいなら
`savedStateHandle.getStateFlow("sortOrder", SortOrder.UPDATED_DESC)` にする(enum は Bundle に入る)。

## 2 「ピン留め」フィールドを追加する

通す順番は Data 層 → モデル → Repository → ViewModel → UI。下から上へ。

```kotlin
// data/local/NoteEntity.kt
@Entity(tableName = "notes", indices = [Index(value = ["remote_id"], unique = true)])
data class NoteEntity(
    ...,
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,
)

// data/local/NotesDatabase.kt
@Database(
    entities = [NoteEntity::class],
    version = 2,
    exportSchema = true,                                   // schemas/ に JSON を出す
    autoMigrations = [AutoMigration(from = 1, to = 2)],   // カラム追加だけなら自動で書ける
)
abstract class NotesDatabase : RoomDatabase() { ... }
```

```kotlin
// app/build.gradle.kts: スキーマの出力先を KSP に教える
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

`exportSchema = true` にすると、ビルド時に `app/schemas/com.example.archnotes.data.local.NotesDatabase/1.json` と `2.json` が生成されます。
**1.json が無いと AutoMigration は書けない**ので、version 1 のまま一度ビルドして 1.json を作ってから version 2 に上げます。
schemas/ は Git 管理に入れます(過去のスキーマは Migration の根拠)。

```kotlin
// data/local/NoteDao.kt
@Query("SELECT * FROM notes ORDER BY is_pinned DESC, updated_at DESC")
fun observeAll(): Flow<List<NoteEntity>>

@Query("UPDATE notes SET is_pinned = :pinned WHERE id = :id")
suspend fun setPinned(id: Long, pinned: Boolean)

// model/Note.kt に val isPinned: Boolean = false を追加、asModel/asEntity で変換
// data/NotesRepository.kt に suspend fun setPinned(id: Long, pinned: Boolean)
```

**同期で消えないように守る場所**: `NoteDao.syncFromRemote` です。
サーバー由来の `NoteEntity` は `isPinned = false` で作られるので、そのまま upsert すると既存行のピン留めが消えます。
`remoteId` を引き継ぐのと同じ発想で、ローカル行の値を引き継ぎます。

```kotlin
@Transaction
suspend fun syncFromRemote(notes: List<NoteEntity>) {
    for (note in notes) {
        val local = note.remoteId?.let { getByRemoteId(it) }
        upsert(note.copy(id = local?.id ?: 0, isPinned = local?.isPinned ?: false))
    }
}
```

```kotlin
// OfflineFirstNotesRepositoryTest.kt
@Test
fun `同期してもローカルのピン留めは消えない`() = runTest {
    remoteNotes = listOf(NetworkNote(id = 1, title = "元", body = "", updatedAt = 10))
    repository.refresh()
    val id = repository.observeNotes().first().single().id
    repository.setPinned(id, true)

    remoteNotes = listOf(NetworkNote(id = 1, title = "更新", body = "", updatedAt = 20))
    repository.refresh()

    val note = repository.observeNote(id).first()!!
    assertEquals("更新", note.title)
    assertTrue(note.isPinned)
}
```

この問題の教訓: 「サーバーが正」の同期でも、**ローカルにしかない情報は同期で守る必要がある**。
どのフィールドがサーバー由来で、どれがローカル由来かを Entity のコメントに書いておくと、後から足す人が迷いません。

UI は `NoteRow` に `IconButton(onClick = { onTogglePin(note.id) }) { Icon(if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, ...) }`
(`material-icons-extended` が必要。または `Icons.Default.Star` で代用)。

## 3 同期失敗時に「再試行」できる Snackbar

`strings.xml` に `<string name="notes_retry">再試行</string>` を追加したうえで:

```kotlin
// NotesScreen.kt(状態なし版)
fun NotesScreen(
    ...,
    onRetry: () -> Unit,     // 追加(2 つの Preview にも onRetry = {} を足す)
) {
    ...
    val userMessage = uiState.userMessageRes?.let { stringResource(it) }
    val retryLabel = stringResource(R.string.notes_retry)
    val canRetry = uiState.userMessageRes == R.string.notes_refresh_failed
    LaunchedEffect(uiState.userMessageRes) {
        if (userMessage != null) {
            onUserMessageShown()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = userMessage,
                    actionLabel = if (canRetry) retryLabel else null,
                )
                if (result == SnackbarResult.ActionPerformed) onRetry()
            }
        }
    }
}

// 状態あり版: onRetry = viewModel::refresh
```

**UDF に反していないか**: 反していません。「再試行が押された」はユーザー操作なので、
UI → ViewModel へ流れる**イベント**です。ViewModel はそれを受けて `refresh()` を実行し、
その結果が状態として UI に戻ります。「Snackbar を出す」が状態で、「アクションが押された」がイベント、という役割分担です。

「どのメッセージなら再試行できるか」を `userMessageRes == R.string.notes_refresh_failed` で UI が判定しているのが気になる場合は、
`NotesUiState` に `userMessage: UserMessage?`(`data class UserMessage(@StringRes val textRes: Int, val canRetry: Boolean)`)
を持たせ、判断を ViewModel 側に寄せます。これが公式ガイドの「ID 付きメッセージ」方式への第一歩です。

## 4 MockWebServer のテストを増やす

```kotlin
@Test
fun `空配列は空リストになる`() = runTest {
    server.enqueue(MockResponse().setBody("[]"))
    assertEquals(emptyList<NetworkNote>(), dataSource.fetchNotes())
}

@Test
fun `必須フィールドが欠けていると IOException になる`() = runTest {
    server.enqueue(MockResponse().setBody("""[{"id": 1, "body": "no title"}]"""))
    assertFetchFailsWithIOException()
}

@Test
fun `応答がないときはタイムアウトで IOException になる`() = runTest {
    val client = OkHttpClient.Builder().readTimeout(200, TimeUnit.MILLISECONDS).build()
    val api = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(client)
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NotesApi::class.java)
    val slowDataSource = RetrofitNoteRemoteDataSource(api)
    server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

    try {
        slowDataSource.fetchNotes()
        fail()
    } catch (e: IOException) {
        // SocketTimeoutException は IOException の派生
    }
}
```

`title` が欠けた要素は `MissingFieldException`(`SerializationException` の派生)になり、
`RetrofitNoteRemoteDataSource` が IOException に変換します。

**本番にタイムアウト設定がないのは問題か**: OkHttp のデフォルトは接続・読み取り・書き込みとも 10 秒です。
多くのアプリではそれで十分ですが、「設定されていない」ことと「意図して 10 秒にした」ことは違います。
`NetworkModule` に明示的に `.connectTimeout(10, SECONDS).readTimeout(10, SECONDS)` と書いておくと、
読む人が「考えた上で 10 秒」だと分かります。

## 5 契約を破るとどうなるか

`catch (e: HttpException)` を消すと、`HTTP エラー(5xx)は IOException に変換される` が
**`retrofit2.HttpException` を投げて落ちます**(`fail()` に到達する前に例外で終了)。

本番では `NotesViewModel.refresh()` の `catch (e: IOException)` を素通りして `viewModelScope` に届き、
捕まえる人がいないので**アプリがクラッシュ**します。Fake で動かしていた間は HttpException は存在しなかったので、
Retrofit に差し替えた瞬間に初めて起きるクラッシュです。

これが「DataSource が契約の例外に変換する」理由であり、MockWebServer のテストが
**ViewModel を変えずに実装を差し替えられること**を保証している、という関係です。

## 6 Hilt のテストで RemoteModule を差し替える(概略)

```toml
# gradle/libs.versions.toml に追加
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }       # BOM が版を決める
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

```kotlin
// build.gradle.kts
androidTestImplementation(libs.hilt.android.testing)
kspAndroidTest(libs.hilt.compiler)
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.ui.test.junit4)
debugImplementation(libs.androidx.ui.test.manifest)
// defaultConfig.testInstrumentationRunner = "com.example.archnotes.HiltTestRunner"

// androidTest/.../HiltTestRunner.kt
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}

// androidTest/.../TestRemoteModule.kt
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RemoteModule::class])
abstract class TestRemoteModule {
    @Binds abstract fun bind(impl: TestNoteRemoteDataSource): NoteRemoteDataSource
}

@Singleton
class TestNoteRemoteDataSource @Inject constructor() : NoteRemoteDataSource {
    override suspend fun fetchNotes() = listOf(NetworkNote(id = 99, title = "テスト用", body = "", updatedAt = 1))
}

// androidTest/.../SyncTest.kt
@HiltAndroidTest
class SyncTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun syncShowsRemoteNote() {
        composeRule.onNodeWithContentDescription("サーバーと同期").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("テスト用").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
```

`replaces = [RemoteModule::class]` は flavor ごとにクラスが違うので、`demo` でテストを走らせる前提で
`src/demo` の `RemoteModule` を指します(テストソースセットも `androidTestDemo` に置くと確実)。
これで「本番コードは 1 文字も変えずに、テストだけ別のデータソースで動かす」が完成します。
