# Step 5: Compose 化 — UI を丸ごと入れ替えても ViewModel は無変更

モジュール: `step5-compose`(比較対象: `step4-hilt-test`)

## 前のステップの課題

- UI が XML + `BaseAdapter`。`notifyDataSetChanged()` で全描き直し
- `findViewById`、`setOnCheckedChangeListener(null)` のようなリサイクル対策など、View 固有の定型コードが多い

## 何を変えたか

```
step5-compose/src/
├── main/java/com/example/legacytodo/
│   ├── MainActivity.kt           ComponentActivity + setContent { TodoScreen() }(コード部分は 10 行ほど)
│   ├── ui/TodoScreen.kt          ★新設: activity_main.xml + item_todo.xml + TodoAdapter + render() の代わり
│   ├── ui/TodoViewModel.kt       ★無変更(Step 4 のファイルをそのままコピー)
│   ├── data/, di/, model/        ★無変更(同上)
│   └── res/layout/               ★削除
└── test/                         ★無変更(そのまま通る)
```

依存関係では `appcompat` と `activity-ktx` が消え、Compose 関連が入りました。
テーマも AppCompat のものから `android:Theme.Material.Light.NoActionBar` に変わるため、Step 0〜4 にあった
アクションバー(アプリ名の帯)は表示されなくなります。必要なら Compose の `TopAppBar` を置きます。

## なぜ ViewModel を触らずに済むのか

Step 2 で `TodoUiState` を設計したとき、**UI 技術に依存するものを一切入れませんでした**。
`List<Todo>`、`Boolean`、`@StringRes Int`。View も Compose も出てこない。
だから View でも Compose でも、同じ `uiState` を購読して描くだけで済みます。

| | View (Step 4) | Compose (Step 5) |
|---|---|---|
| 状態の購読 | `repeatOnLifecycle(STARTED) { uiState.collect { render(it) } }` | `uiState.collectAsStateWithLifecycle()` |
| 一覧の描画 | `adapter.submit(state.todos)` | `LazyColumn { items(uiState.todos) { ... } }` |
| 集計の表示 | `summaryText.text = if (state.isLoading) ... else getString(...)` | `Text(if (uiState.isLoading) ... else stringResource(...))` |
| エラー表示 | `Toast` + `errorMessageShown()` | `onErrorShown()` + `SnackbarHost`(画面スコープで表示) |
| イベント | `viewModel.addTodo(input.text.toString())` | `onAdd(inputText)` |

右の列は左の列を Compose で書き直しただけで、ViewModel の API は 1 つも変わっていません。
`TodoViewModelTest` がそのまま通ることが、ロジックが退行していない証拠になります。

## Compose 側の設計ポイント

### 状態あり版 / 状態なし版

`TodoScreen(viewModel)` は ViewModel とつなぐだけ。`TodoScreen(uiState, onAdd, ...)` は描くだけ。
後者は `@Preview` で任意の状態を渡して見た目を確認できます(ファイル末尾)。

### 入力中のテキストは UI 側に置いた

`var inputText by rememberSaveable { mutableStateOf("") }` と、Composable の中で持っています。
この画面の入力文字列は、追加ボタンでイベントの引数として一度 ViewModel に渡すだけで、ViewModel が保持する必要がありません。
そういう状態は UI 側に置いてよく、`rememberSaveable` にしておけば画面回転でもプロセス再生成でも消えません。
一方「空文字は追加できない」という業務ルールは ViewModel にあります。
ViewModel の処理が入力値を必要とする場合(01-new-app の編集画面)は ViewModel 側に置きます。
この線引きは [docs/02-layers.md](../../docs/02-layers.md) の UI 層ルール 6 にまとめてあります。

### エラー表示は「受け取ったらすぐ消してもらう」

`uiState.errorMessageRes` が非 null になったら、まず `onErrorShown()` で ViewModel の状態を消し、
それから画面のスコープで Snackbar を表示します(`LaunchedEffect` + `rememberCoroutineScope`)。
Snackbar が消えるまで状態を残しておくと、その間に回転すると再表示され、同じエラーをもう一度起こしても
StateFlow が同じ値を流さないので表示されない、という 2 つの問題が起きます。
Step 2〜4 の Toast は表示と同時に状態を消していたので、Compose でも同じタイミングにそろえています。
代わりに、表示中に回転すると Snackbar は消えて再表示されません(受け取り済みなので)。
これを厳密に扱うなら公式ガイドの「ID 付きメッセージのリスト」方式になります([docs/03-decision-guide.md](../../docs/03-decision-guide.md) の Q6)。
Step 2 で「イベントも状態として表す」と決めたので、Compose でも同じ考え方で扱えます。

## ここまでの到達点

Step 0 の 150 行の Activity は、次のように分かれました。

| 責務 | 置き場所 | テスト方法 |
|---|---|---|
| 描画 | `TodoScreen`(状態なし版) | Preview / Compose UI テスト |
| 状態と業務ロジック | `TodoViewModel` + `TodoUiState` | JVM 単体テスト(あり) |
| データの読み書き | `TodoRepository` / `RoomTodoRepository` | Fake に差し替え可能 |
| SQL | `TodoDao` | Instrumented Test(01-new-app に例あり) |
| 組み立て | `di/` | — |

これが [01-new-app](../../01-new-app/README.md) の構成と同じ形になっていることを確認してください。
新規アプリに最初から入れた場合と、古いアプリを直した場合で、到達点は同じです。

## この先

- 画面が増えたら: `ui/` 配下に画面ごとのパッケージを作り、`NavHost` で遷移を定義する(01-new-app 参照)。
- サーバー同期が必要になったら: `TodoRemoteDataSource` を作り、`RoomTodoRepository` の中で Room と組み合わせる(オフラインファースト)。UI は変わらない。
- ロジックを複数画面で使うようになったら: `domain/` に UseCase を切り出す。
