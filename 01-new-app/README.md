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
│   ├── navigation/ArchNotesNavHost.kt   画面遷移。型安全ルート
│   ├── notes/
│   │   ├── NotesViewModel.kt     NotesUiState + 状態ホルダー
│   │   └── NotesScreen.kt        状態あり版 / 状態なし版の Composable
│   ├── editor/
│   │   ├── NoteEditorViewModel.kt   SavedStateHandle の使い方
│   │   └── NoteEditorScreen.kt
│   └── theme/Theme.kt
├── domain/                       ── Domain 層(省略可) ──
│   └── GetNotesUseCase.kt        検索語でフィルタする
└── data/                         ── Data 層 ──
    ├── NotesRepository.kt        公開 API(インターフェース)
    ├── OfflineFirstNotesRepository.kt   Room を SSOT とする実装
    ├── local/                    Room: NoteEntity, NoteDao, NotesDatabase
    ├── remote/                   NoteRemoteDataSource(抽象)と Fake 実装
    └── di/                       Hilt モジュール: 何をどう組み立てるか

app/src/test/        JVM 単体テスト(ViewModel × 2, UseCase)と Fake / テストルール
app/src/androidTest/ Room DAO の Instrumented Test
```

依存の向き: `ui → domain → data`。逆方向の import は 1 つもありません。
`model/Note` だけはすべての層から参照されます。

## 読む順番

1. `model/Note.kt` — 2 分。これが何を表すかを把握する。
2. `data/NotesRepository.kt` — インターフェースのコメントを読む。「読み取りは Flow、書き込みは suspend」の理由。
3. `data/OfflineFirstNotesRepository.kt` — SSOT とオフラインファースト。`refresh()` がサーバーの応答を UI に返さず Room に書く点に注目。
4. `data/local/` と `data/remote/` — Entity / DTO / モデルを分ける理由。
5. `domain/GetNotesUseCase.kt` — Domain 層の形と、置く/置かないの判断。
6. `ui/notes/NotesViewModel.kt` — 複数の StateFlow を `combine` して 1 つの UiState にする。例外を状態に変換する。
7. `ui/notes/NotesScreen.kt` — 状態あり版と状態なし版。「一度きりの通知」の扱い。
8. `ui/editor/NoteEditorViewModel.kt` — SavedStateHandle。プロセス再生成後にユーザーの編集を上書きしない工夫。
9. `data/di/` — Hilt モジュール。Fake と本物の差し替えが 1 行で済むことを確認。
10. `src/test/` — ViewModel が JVM だけでテストできることを確認。

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
3. `notesRepository.refresh()` → `withContext(ioDispatcher)` でバックグラウンドへ
4. `FakeNoteRemoteDataSource.fetchNotes()` が 1.2 秒待って 3 件返す(3 回に 1 回 `IOException`)
5. 成功: `noteDao.upsertAll(...)` で Room に書く → Room の `observeAll()` Flow が新しいリストを流す → 6〜8 と同じ経路で UI が更新される。
   **ViewModel は Room の結果を受け取っていない**ことに注目。書いたら勝手に流れてくる。
6. 失敗: ViewModel が `IOException` を catch → `userMessageRes.value = R.string.notes_refresh_failed` → UiState → UI が Snackbar を出す → `onUserMessageShown()` → `userMessageRes.value = null`
7. `finally` で `isRefreshing.value = false`

## 単体テストの読み方

- `testing/MainDispatcherRule.kt`: `viewModelScope` が使う `Dispatchers.Main` をテスト用に差し替える定番。
- `testing/FakeNotesRepository.kt`: Room とネットワークの代わり。`shouldFailRefresh = true` でエラー経路も検証できる。
- `NotesViewModelTest`: `stateIn(WhileSubscribed)` は購読者がいないと動かないので、`backgroundScope.launch { uiState.collect() }` で購読してから検証する。
- `NoteEditorViewModelTest`: 画面引数は `SavedStateHandle(mapOf("noteId" to 7L))` で渡す。

## 本物のアプリにするとき

- `FakeNoteRemoteDataSource` を Retrofit 実装に置き換え、`DataModule` の `@Binds` 先を変える。他は触らない。
- `NotesDatabase` の `exportSchema = true` にしてスキーマを Git 管理し、変更時は Migration を書く。
- 画面が増えて同じロジックが 2 か所に現れたら UseCase へ。
- ビルドが遅くなったら `:core:data` `:feature:notes` のようにモジュールへ切り出す(パッケージ構成がそのまま使える)。
