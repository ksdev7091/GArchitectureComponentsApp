# 01-new-app: ArchNotes — 新規アプリに推奨アーキテクチャを最初から導入する

メモ帳アプリ。一覧・検索・作成・編集・削除と、疑似サーバーからの同期ができます。
機能は意図的に小さく、**推奨アーキテクチャの部品がすべて 1 回ずつ登場する**ことを優先しています。

## 画面

- **一覧画面** (`NotesScreen`): 検索ボックス、メモのリスト、削除ボタン、右上の同期ボタン、右下の追加ボタン。
  同期は 1.2 秒かかり、3 回に 1 回は失敗して Snackbar が出ます(エラー処理の学習用)。
- **編集画面** (`NoteEditorScreen`): タイトルと本文。保存すると一覧へ戻ります。

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
│   │   ├── NotesNavigation.kt    一覧画面のルートと NavGraph 登録関数
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
    ├── remote/                   NoteRemoteDataSource(抽象、失敗は IOException)と Fake 実装
    └── di/                       Hilt モジュール: 何をどう組み立てるか

app/src/test/        JVM 単体テスト 20 件(ViewModel × 2, UseCase, Repository)と Fake / テストルール
app/src/androidTest/ Room DAO の Instrumented Test
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
4. `data/local/` と `data/remote/` — Entity / DTO / モデルを分ける理由。ローカル ID とサーバー ID を別カラムに持つ理由。
   `NoteDao.syncFromRemote` がサーバー ID で照合しながら 1 トランザクションで書く理由。
   DataSource が「失敗は IOException」という契約を持つ理由。
5. `domain/GetNotesUseCase.kt` — Domain 層の形と、置く/置かないの判断。
6. `ui/notes/NotesViewModel.kt` — 複数の StateFlow を `combine` して 1 つの UiState にする。例外を状態に変換する。
7. `ui/notes/NotesScreen.kt` — 状態あり版と状態なし版。「一度きりの通知」の扱い(受け取ったらすぐ消してもらう)。
8. `ui/editor/NoteEditorViewModel.kt` — 画面引数、入力テキストをスナップショット状態 + `savedStateHandle.saveable` で持つ理由、
   プロセス再生成後にユーザーの編集を上書きしない工夫、保存ボタン連打の防止。
   「保存できるか」をスナップショット状態と StateFlow の両方から導く値として画面側で計算している理由。
9. `ui/notes/NotesNavigation.kt`、`ui/editor/NoteEditorNavigation.kt`、`ui/ArchNotesNavHost.kt` — ルートを画面側に置く理由。
   行の連打で画面が 2 枚積まれないよう、遷移元が RESUMED のときだけ遷移する仕組み。
10. `data/di/` — Hilt モジュール。Fake と本物の差し替えが 1 行で済むことを確認。
11. `src/test/` — ViewModel と Repository が JVM だけでテストできることを確認。

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
3. `notesRepository.refresh()` → `FakeNoteRemoteDataSource.fetchNotes()` が 1.2 秒待って 3 件返す(3 回に 1 回 `IOException`)。
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

## 本物のアプリにするとき

- `FakeNoteRemoteDataSource` を Retrofit 実装に置き換え、`DataModule` の `@Binds` 先を変える。
  その実装の中で `HttpException` などを `IOException` に変換して契約を守れば、Repository と ViewModel は触らなくてよい。
- 同期は「サーバーが正」の単純な上書き。ローカルの編集をサーバーへ送る、競合を解決する、といった双方向同期は別途設計が必要。
- `NotesDatabase` の `exportSchema = true` にしてスキーマを Git 管理し、変更時は Migration を書く。
- 画面が増えて同じロジックが 2 か所に現れたら UseCase へ。
- ビルドが遅くなったら `:core:data` `:feature:notes` のようにモジュールへ切り出す(パッケージ構成と依存の向きがそのまま使える)。
