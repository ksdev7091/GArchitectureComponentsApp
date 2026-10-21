# レガシーのバリエーションと、既存 DB を Room に引き継ぐ手順

Step 0 は「生の Thread + SQLiteOpenHelper + 1 つの Activity」という最も素朴なレガシーです。
実際の案件で出会うレガシーは Fragment、AsyncTask、LiveData + DataBinding、RxJava など形が違いますが、
**直す順番と考え方は Step 1〜5 と同じ**です。ここでは「自分のアプリの場合はどこを読み替えるか」をまとめます。

## 1. 読み替え表

| 手元のレガシーにあるもの | Step 0 での相当物 | どのステップで、何に置き換えるか |
|---|---|---|
| `AsyncTask` / `Thread` + `Handler` / `runOnUiThread` | `Thread` + `runOnUiThread` | Step 1: `suspend` 関数 + `lifecycleScope` / `viewModelScope`。`AsyncTask.doInBackground` の中身が Repository の `withContext(IO)` に、`onPostExecute` が `launch` の続きになる |
| `SQLiteOpenHelper` + `Cursor` | `TodoDbHelper` | Step 1 で Repository に隠し、Step 3 で Room に。本番データがあるなら 2 節の手順で引き継ぐ |
| `Volley` / `HttpURLConnection` / 古い `OkHttp` コールバック | (Step 0 には無い) | Step 1 で `RemoteDataSource` として Repository の裏に隠す。コールバック API は `suspendCancellableCoroutine` で suspend 化するか、Retrofit に置き換える(01-new-app の `RetrofitNoteRemoteDataSource` が完成形) |
| `Fragment` に全部書いてある | `Activity` に全部書いてある | 考え方は同じ。Fragment では `viewLifecycleOwner.lifecycleScope` と `viewLifecycleOwner.repeatOnLifecycle` を使う(Fragment 自身の lifecycle ではなく **View の** lifecycle。View は Fragment より先に破棄される) |
| 複数 Fragment で同じデータを使う | (Step 0 には無い) | ViewModel を `activityViewModels()` で共有するか、同じ Repository を各 ViewModel が購読する。後者が推奨(Repository が SSOT。画面間の受け渡しが不要になる) |
| `LiveData` + `Observer` | (Step 2 の `StateFlow` に相当) | すでに「状態を購読して描く」形なので、Step 2 は半分終わっている。新規は `StateFlow`。既存の `LiveData` を一気に置き換える必要はなく、`asLiveData()` / `asFlow()` で橋渡ししながら画面単位で移行する |
| `MutableLiveData` を複数持つ ViewModel | (Step 2 で `TodoUiState` にまとめた) | 関連する状態を 1 つの `UiState` data class にまとめる(02-layers の UI 層ルール 1)。`MediatorLiveData` でやっていた合成は `combine` になる |
| `DataBinding`(`@{viewModel.xxx}`) | (Step 2 の `render()`) | Compose 化(Step 5)するなら不要になる。View のまま続けるなら `ViewBinding` + `render(state)` に寄せると、ロジックが XML から Kotlin に戻って追いやすい |
| `RxJava`(`Observable` / `Single`) | (Flow に相当) | `Single` → `suspend fun`、`Observable` / `Flowable` → `Flow`。`kotlinx-coroutines-rx3` の `await()` / `asFlow()` で橋渡しし、Repository の境界で Rx を止める(UI 層に Rx を入れない) |
| `SharedPreferences` | (Step 0 には無い) | `DataStore`(01-new-app の `DataStoreUserPreferencesRepository`)。読み取りが Flow になるので、設定変更が自動で画面に届く |
| `Singleton` オブジェクトでデータを共有(`object AppData`) | (Step 0 には無い) | Repository に置き換える。グローバルな可変状態を Flow を公開する Repository に変え、書き換えは Repository のメソッド経由に限定する |
| `static` な `Context` 参照、`Application` から取り出す DI もどき | (Step 2 の `AppContainer`) | Step 2 の手動 DI → Step 4 の Hilt |

## 2. 既存の SQLite DB を Room に引き継ぐ

Step 3 では各ステップが別アプリなので DB を作り直しましたが、本番アプリでは **ユーザーのデータを消せません**。
Room は開いた DB のスキーマを検証するため、何もしないと起動時に例外になります。

### 手順

1. **現在のテーブル定義を正確に調べる**: `sqlite3` か Database Inspector で `SELECT sql FROM sqlite_master` を実行し、
   `CREATE TABLE` 文をメモする。Room の検証は列名・型・NOT NULL・主キー・AUTOINCREMENT・インデックスまで見る。

2. **Entity を旧テーブルに合わせて書く**(Step 3 の `TodoEntity` はこの形):
   ```kotlin
   @Entity(tableName = "todos")
   data class TodoEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,          // INTEGER PRIMARY KEY AUTOINCREMENT
       val title: String,                                          // TEXT NOT NULL
       @ColumnInfo(name = "is_done") val isDone: Boolean = false,  // INTEGER NOT NULL
       @ColumnInfo(name = "created_at") val createdAt: Long,       // INTEGER NOT NULL
   )
   ```
   旧テーブルが `is_done INTEGER NOT NULL DEFAULT 0` でも、Room の検証は DEFAULT を見ないので一致扱いになる。

3. **DB 名と version を引き継ぐ**: `Room.databaseBuilder(context, TodoDatabase::class.java, "todo.db")`(旧 `SQLiteOpenHelper` と同じファイル名)。
   `@Database(version = 1)` を旧 `SQLiteOpenHelper` の version と同じにする。

4. **スキーマが完全に一致するなら、これだけで引き継げる**: Room は `room_master_table` が無い DB を開くと、
   Entity から期待するスキーマと実際のテーブルを比較し、一致すれば自分の管理下に置く。一致しなければ
   `IllegalStateException: Migration didn't properly handle ...` 系の例外で落ちる。

5. **一致しない(列を足す・型を変える)なら Migration を書く**: version を上げ、旧スキーマ → 新スキーマへの SQL を書く。
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           // 例: 期限列を追加
           db.execSQL("ALTER TABLE todos ADD COLUMN due_at INTEGER")
       }
   }
   Room.databaseBuilder(context, TodoDatabase::class.java, "todo.db")
       .addMigrations(MIGRATION_1_2)
       .build()
   ```
   SQLite の `ALTER TABLE` は列の追加しかできない。型変更や NOT NULL の追加は
   「新テーブルを作る → `INSERT INTO new SELECT ... FROM old` → 旧テーブルを DROP → RENAME」の 4 手順になる。

6. **`exportSchema = true` にしてスキーマを Git 管理する**(`ksp { arg("room.schemaLocation", "$projectDir/schemas") }`)。
   以後の変更は `@AutoMigration(from = 1, to = 2)` で自動生成できることが多い(列の追加・削除・テーブル名変更)。

7. **Migration をテストする**: `androidx.room:room-testing` の `MigrationTestHelper` で、
   旧 version の DB を作り、データを入れ、Migration を適用して中身が残っていることを検証する。
   ```kotlin
   @get:Rule
   val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), TodoDatabase::class.java)

   @Test
   fun migrate1To2_keepsRows() {
       helper.createDatabase("test.db", 1).apply {
           execSQL("INSERT INTO todos (title, is_done, created_at) VALUES ('a', 0, 1)")
           close()
       }
       val db = helper.runMigrationsAndValidate("test.db", 2, true, MIGRATION_1_2)
       db.query("SELECT title FROM todos").use { c -> c.moveToFirst(); assertEquals("a", c.getString(0)) }
   }
   ```
   `runMigrationsAndValidate` は Migration 後のスキーマが Entity と一致するかも検証するので、
   「手で書いた SQL が Room の期待とずれている」ミスをここで捕まえられる。

### やってはいけないこと

- `fallbackToDestructiveMigration()` を本番に残す。スキーマがずれた瞬間にユーザーのデータが消える。開発中の一時しのぎに限る。
- 旧 `SQLiteOpenHelper` と Room を同じファイルに対して同時に使う。片方の書き込みをもう片方が検知できず、ロックの競合も起きる。
  Step 1 → Step 3 のように、まず Repository の裏にすべての DB アクセスを集めてから、一度に Room へ切り替える。

## 3. Fragment 版 Step 0 を自分で作ってみる(演習)

`step0-legacy` を Fragment ベースに書き換えてみてください(`MainActivity` が `TodoListFragment` を置き、Fragment に全部書く)。
その状態で Step 1〜2 を適用すると、次の違いが体感できます。

- `lifecycleScope` → `viewLifecycleOwner.lifecycleScope`、`repeatOnLifecycle` → `viewLifecycleOwner.repeatOnLifecycle`。
  Fragment の lifecycle で購読すると、View が破棄されたあとに `findViewById` した View を触ってクラッシュする。
- `by viewModels()` は Fragment ごと。回転で Fragment が作り直されても ViewModel は残る(Activity と同じ)。
- Fragment が 2 つになって同じ Todo を表示するとき、Step 3 以降なら両方が同じ Room の Flow を購読するだけで一致する。
  Step 2 以前なら、片方で追加したものをもう片方に伝える仕組みを自分で作ることになる(これが SSOT の価値)。
