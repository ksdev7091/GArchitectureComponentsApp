# 01-new-app: ArchNotes — 新規アプリに推奨アーキテクチャを最初から導入する

メモ帳アプリ。一覧・検索・作成・編集・削除と、サーバーからの同期ができます。
機能は意図的に小さく、**推奨アーキテクチャの部品がすべて 1 回ずつ登場する**ことを優先しています。

## 画面

- **一覧画面** (`NotesScreen`): 検索ボックス、メモのリスト、削除ボタン、右上の同期ボタン、右下の追加ボタン。
- **編集画面** (`NoteEditorScreen`): タイトルと本文。保存すると一覧へ戻ります。

## 2 つの flavor: demo と prod

リモートデータソースの実装を product flavor で切り替えます(公式サンプル Now in Android と同じ方式)。
Android Studio の **Build Variants** ウィンドウで `demoDebug` / `prodDebug` を選びます。

| flavor | リモートの実装 | 同期ボタンの挙動 |
|---|---|---|
| `demo`(既定) | `FakeNoteRemoteDataSource`(ネットワークに出ない) | 1.2 秒後に 3 件のメモを取り込む。3 回に 1 回は失敗して Snackbar が出る |
| `prod` | `RetrofitNoteRemoteDataSource`(Retrofit + OkHttp) | 公開 API [JSONPlaceholder](https://jsonplaceholder.typicode.com/posts) から 100 件の投稿をメモとして取り込む(要インターネット)。この API は更新日時を返さないので、同期のたびに 100 件の更新日時が取り込み時刻になる |

両者の違いは `src/demo` と `src/prod` にある `RemoteModule`(Hilt)の 1 行だけで、Repository / ViewModel / UI は共通です。
Retrofit 実装の正しさは、端末なしで動く MockWebServer のテスト(`RetrofitNoteRemoteDataSourceTest`)で検証しています。

## 構成

```
app/src/main/java/com/example/archnotes/
├── ArchNotesApplication.kt       @HiltAndroidApp
├── MainActivity.kt               Compose を立ち上げるだけ
├── model/
│   └── Note.kt                   アプリ全体で使うモデル(層をまたぐ唯一のデータ型)
├── ui/                           ── UI 層 ──
│   ├── ArchNotesNavHost.kt       画面遷移。各画面の登録関数を呼ぶだけ
│   ├── notes/
│   │   ├── NotesNavigation.kt    一覧画面のルートと NavGraph 登録関数(連打対策もここ)
│   │   ├── NotesViewModel.kt     NotesUiState + 状態ホルダー
│   │   └── NotesScreen.kt        状態あり版 / 状態なし版の Composable
│   ├── editor/
│   │   ├── NoteEditorNavigation.kt  編集画面のルート、遷移関数、NavGraph 登録関数
│   │   ├── NoteEditorViewModel.kt   SavedStateHandle、スナップショット状態、二重保存防止
│   │   └── NoteEditorScreen.kt
│   └── theme/Theme.kt
├── domain/                       ── Domain 層(省略可) ──
│   └── GetNotesUseCase.kt        検索語でフィルタする
└── data/                         ── Data 層 ──
    ├── NotesRepository.kt        公開 API(インターフェース)
    ├── OfflineFirstNotesRepository.kt   Room を SSOT とする実装
    ├── local/                    Room: NoteEntity(id と remoteId), NoteDao(remoteId 照合の同期は @Transaction), NotesDatabase
    ├── remote/
    │   ├── NoteRemoteDataSource.kt      抽象 + DTO(NetworkNote)。失敗は IOException という契約
    │   ├── NotesApi.kt                  Retrofit の API 定義
    │   ├── RetrofitNoteRemoteDataSource.kt  本物の HTTP。実装固有の例外を契約の例外に変換する
    │   └── FakeNoteRemoteDataSource.kt  疑似リモート
    └── di/                       Hilt モジュール: DataModule(Repository)、DatabaseModule(Room)、NetworkModule(Retrofit)
app/src/demo/java/.../data/di/RemoteModule.kt   demo: Fake を選ぶ
app/src/prod/java/.../data/di/RemoteModule.kt   prod: Retrofit を選ぶ

app/src/test/        JVM 単体テスト 25 件(ViewModel × 2, UseCase, Repository, Retrofit + MockWebServer)と Fake / テストルール
app/src/androidTest/ Room DAO の Instrumented Test
docs/                演習問題(exercises.md)と解答例(answers.md)
```

依存の向き: `ui → domain → data`。逆方向の import は 1 つもありません。
UI 層が Data 層を直接使うのは許されています(`NotesViewModel` は削除・同期で Repository を直接呼ぶ)。
`model/Note` だけはすべての層から参照されます。
ルート定義は各画面のパッケージが持ち、`ArchNotesNavHost` がそれに依存します(画面側は NavHost を知らない)。

## 読む順番

1. `model/Note.kt` — 2 分。これが何を表すかを把握する。
2. `data/NotesRepository.kt` — インターフェースのコメントを読む。「読み取りは Flow、書き込みは suspend」の理由。
3. `data/OfflineFirstNotesRepository.kt` — SSOT とオフラインファースト。`refresh()` がサーバーの応答を UI に返さず Room に書く点、
   `saveNote` が `@Upsert` の戻り値の癖と `remoteId` の引き継ぎを吸収している点、`withContext` が不要な理由に注目。
4. `data/local/` — Entity とモデルを分ける理由。ローカル ID とサーバー ID を別カラムに持つ理由。
   `NoteDao.syncFromRemote` がサーバー ID で照合しながら 1 トランザクションで書く理由。
5. `data/remote/` — DTO(`@SerialName`、省略可能なフィールド)、Retrofit の API 定義、
   `RetrofitNoteRemoteDataSource` が `HttpException` / `SerializationException` を `IOException` に変換する理由。
   `src/demo` と `src/prod` の `RemoteModule` を見比べ、差し替えが 1 行で済むことを確認する。
6. `domain/GetNotesUseCase.kt` — Domain 層の形と、置く/置かないの判断。
7. `ui/notes/NotesViewModel.kt` — 複数の StateFlow を `combine` して 1 つの UiState にする。例外を状態に変換する。
8. `ui/notes/NotesScreen.kt` — 状態あり版と状態なし版。「一度きりの通知」の扱い(受け取ったらすぐ消してもらう)。
9. `ui/editor/NoteEditorViewModel.kt` — 画面引数、入力テキストをスナップショット状態 + `savedStateHandle.saveable` で持つ理由、
   プロセス再生成後にユーザーの編集を上書きしない工夫、保存ボタン連打の防止。
   「保存できるか」をスナップショット状態と StateFlow の両方から導く値として画面側で計算している理由。
10. `ui/notes/NotesNavigation.kt`、`ui/editor/NoteEditorNavigation.kt`、`ui/ArchNotesNavHost.kt` — ルートを画面側に置く理由。
    行の連打で画面が 2 枚積まれないよう、遷移元が RESUMED のときだけ遷移する仕組み。
11. `data/di/` — Hilt モジュール。`NetworkModule` の組み立てと、テストで同じ組み立てを MockWebServer に向けてやり直す対応関係。
12. `src/test/` — ViewModel、Repository、ネットワーク層が JVM だけでテストできることを確認。
13. [docs/exercises.md](docs/exercises.md) — 機能を足す演習で、各層をどう通すかを手に入れる。

## データの流れを追う: 検索語を 1 文字入力したとき

1. `NotesScreen`(状態なし版)の `OutlinedTextField.onValueChange` → `onQueryChange("k")`
2. 状態あり版が `viewModel::onQueryChange` を渡しているので `NotesViewModel.onQueryChange("k")` が呼ばれる
3. `query.value = "k"`(内部の `MutableStateFlow` を更新)
4. `notes = query.flatMapLatest { getNotes(it) }` が反応し、`GetNotesUseCase("k")` が新しい Flow を返す
5. `GetNotesUseCase` は `notesRepository.observeNotes()`(Room の Flow)を `map` でフィルタする
6. `combine(notes, query, isRefreshing, userMessageRes)` が新しい `NotesUiState(notes=[...], query="k")` を作る
7. `stateIn` がそれを `uiState` の最新値にする
8. `collectAsStateWithLifecycle()` が新しい値を受け取り、状態なし版 `NotesScreen` が再コンポーズされる

UI が関わるのは 1 と 8 だけ。「何件に絞られるか」を UI は判断していません。

## データの流れを追う: 同期ボタンを押したとき

1. `IconButton(onClick = onRefresh)` → `NotesViewModel.refresh()`
2. `isRefreshing.value = true` → `combine` が走り UiState の `isRefreshing = true` → UI に `LinearProgressIndicator` が出る
3. `notesRepository.refresh()` → `NoteRemoteDataSource.fetchNotes()`。
   demo なら Fake が 1.2 秒待って 3 件返す(3 回に 1 回 `IOException`)。
   prod なら Retrofit が `GET /posts` を送り、JSON を `List<NetworkNote>` に変換する。HTTP エラーや壊れた JSON は `IOException` に変換される。
   Room も DataSource も main-safe なので、Repository は `withContext` を書いていない
4. 成功: `NoteDao.syncFromRemote` が 1 トランザクションの中で各メモのサーバー ID でローカル行を探し、あればその行、なければ新規行として `upsert`
   → Room の `observeAll()` Flow が新しいリストを流す → 検索の流れの 6〜8 と同じ経路で UI が更新される。
   **ViewModel は Room の結果を受け取っていない**ことに注目。書いたら勝手に流れてくる。
5. 失敗: ViewModel が `IOException` を catch → `userMessageRes.value = R.string.notes_refresh_failed` → UiState
   → UI は `userMessageShown()` で状態を消してから Snackbar を出す
6. `finally` で `isRefreshing.value = false`

## 単体テストの読み方

- `testing/MainDispatcherRule.kt`: `viewModelScope` が使う `Dispatchers.Main` をテスト用に差し替える定番。
- `testing/FakeNotesRepository.kt`: Room とネットワークの代わり。`shouldFailRefresh` でエラー経路、
  `refreshGate` / `saveGate`(`CompletableDeferred`)で「同期中」「保存中」の途中状態を観察できる。
- `testing/FakeNoteDao.kt`: Room の DAO の代わり。`upsert` が UPDATE 時に -1 を返す本物の挙動まで真似ている。
- `NotesViewModelTest`: `stateIn(WhileSubscribed)` は購読者がいないと動かないので、`backgroundScope.launch { uiState.collect() }` で購読してから検証する。
  並び順は本物では DAO の SQL が決めるので、ここでは検証しない(`NoteDaoTest` にある)。
- `NoteEditorViewModelTest`: 画面引数は `SavedStateHandle(mapOf("noteId" to 7L))` で渡す。
  「ロード済みフラグがあれば読み直さない」「連打しても 1 件」を検証している。
- `OfflineFirstNotesRepositoryTest`: 「再同期しても重複しない」「ローカルのメモが消えない」「remoteId を失わない」を検証している。
- `RetrofitNoteRemoteDataSourceTest`: MockWebServer をローカルの HTTP サーバーとして立て、
  JSON の解釈(snake_case、未知のキー、省略フィールド)と、HTTP エラー・壊れた JSON・接続失敗がすべて `IOException` になることを検証している。

## 実機で確認済みの挙動(Android 16)

文書で約束している次の挙動は、実機(Xiaomi、Android 16)で操作して確認しています。
自分で試すときのチェックリストとしても使えます。

- 空タイトルでは保存できない。タイトルを入れると保存でき、一覧に出る
- 編集中に画面を回転しても入力中のテキストが残る(`savedStateHandle.saveable`)
- 編集中にホームへ戻り、プロセスを kill されても(`adb shell run-as <pkg> kill -9 <pid>`)、戻ると編集画面と未保存のテキストが復元され、DB の内容で上書きされない
- 保存せずに戻ると編集は破棄される
- 検索語で一覧が絞られ、消すと戻る
- 同期中は同期ボタンが無効になり、連打しても 2 回目は無視される。demo では 3 回目に失敗の Snackbar が出て数秒で消える。2 回同期しても重複しない
- 削除すると Snackbar が出て一覧から消える
- 行を連打しても編集画面は 1 枚しか積まれない(戻るを 1 回で一覧)
- ダークモードで表示が崩れない
- 本文に長文を入れてもキーボードの下に隠れない(`imePadding`。edge-to-edge ではアプリが IME のインセットを扱う)
- prod: サーバーのメモを編集して再同期すると、サーバーの内容に戻る(「サーバーが正」)。削除しても再同期で戻る。重複しない

## 本物のアプリにするとき

- `NotesApi` のパスとベース URL(`build.gradle.kts` の `API_BASE_URL`)を自分のサーバーに合わせ、`NetworkNote` を API の JSON に合わせる。
  認証ヘッダが必要なら `NetworkModule` の OkHttpClient に Interceptor を足す。他の層は触らない。
- 同期は「サーバーが正」の単純な上書き。ローカルの編集をサーバーへ送る、競合を解決する、といった双方向同期は別途設計が必要。
- `NotesDatabase` の `exportSchema = true` にしてスキーマを Git 管理し、変更時は Migration を書く(演習 2 で実際にやる)。
- 画面が増えて同じロジックが 2 か所に現れたら UseCase へ。
- ビルドが遅くなったら `:core:data` `:feature:notes` のようにモジュールへ切り出す(パッケージ構成と依存の向きがそのまま使える)。
