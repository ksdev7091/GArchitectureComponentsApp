# Step 2: ViewModel と UI 状態を導入する

モジュール: `step2-viewmodel`(比較対象: `step1-repository`)

## 前のステップの課題

- ③ 状態(`todos`)が Activity のフィールドにあり、回転のたびに読み直す
- ④ 完了数の集計・空文字チェックが Activity にある
- ⑤ Repository の組み立てを Activity がやっている
- ① Adapter が Activity の inner class で、Activity のフィールドを直接触っている

## 何を変えたか

```
step2-viewmodel/src/main/java/com/example/legacytodo/
├── MainActivity.kt           状態を描く + イベントを渡す、だけになった
├── TodoApplication.kt        ★新設: AppContainer(手動 DI)
├── model/Todo.kt
├── data/                     (Step 1 と同一)
└── ui/
    ├── TodoViewModel.kt      ★新設: TodoUiState + TodoViewModel
    └── TodoAdapter.kt        ★分離: submit(list) で受け取り、コールバックで返すだけ
```

1. **`TodoUiState` を定義した**
   `todos`、`isLoading`、`errorMessageRes` を持つ不変の data class。
   `doneCount` は `todos` から計算できるので、フィールドではなく計算プロパティ(二重管理しない)。
2. **`TodoViewModel` を作った**
   `MutableStateFlow<TodoUiState>` を持ち、`uiState` として公開する。
   `addTodo` / `setDone` / `deleteTodo` を受け取り、Repository を呼んだあと状態を更新する。
   空文字チェックはここに来た。
3. **MainActivity は `repeatOnLifecycle(STARTED)` で `uiState` を購読し、`render(state)` で描く**
   ユーザー操作は `viewModel.xxx()` を呼ぶだけ。
4. **`AppContainer` を Application に置き、Repository の組み立てを 1 か所に集めた**
   ViewModel の `Factory` がそこから Repository を受け取る。
5. **Adapter を独立したクラスにした**
   `submit(todos)` で表示データを受け取り、操作はコンストラクタで受け取ったコールバックで返す。

## なぜこれで解決するのか

### ViewModel は構成変更を生き延びる

`by viewModels()` で取得した ViewModel は、画面回転で Activity が作り直されても**同じインスタンス**が返ってきます。
だから `todos` を ViewModel に置けば、回転のたびに DB を読み直す必要がない(③の解決)。
Step 1 のアプリと Step 2 のアプリで、データを数件入れてから画面を回転させてみてください。Step 1 は一瞬リストが空になり、Step 2 はなりません。

### 単方向データフロー(UDF)

```
          TodoUiState
  TodoViewModel ──────────▶ MainActivity.render()
               ◀──────────
         addTodo / setDone / deleteTodo
```

- Activity は状態を**受け取って描くだけ**。自分で `todos` を書き換えない。
- Activity はイベントを**伝えるだけ**。何が起きるかを決めない。
- 状態を変えるのは ViewModel だけ。

これで「画面がこうなっている理由」は「`uiState` がこうだから」の一点に絞られます。

### 業務ロジックが UI から出た(④の解決)

空文字チェックと集計が ViewModel / UiState に移りました。
ViewModel は `Context` も `View` も知らないので、**JVM だけで `new` できます**。
つまりテストが書ける形になりました(Step 4 で書きます)。

### 「エラーを表示したい」も状態にする

Step 1 では Activity が直接 `Toast` を出していました。
Step 2 では ViewModel が `errorMessageRes = R.string.empty_title_error` と**状態に入れ**、Activity が `render()` でそれを見て Toast を出し、`errorMessageShown()` で消してもらいます。
文字列ではなくリソース ID にしているのは、ViewModel が `Context` に依存しないためです。

### 手動 DI(AppContainer)

Hilt のような DI ライブラリが何をしているかを理解するために、あえて手書きしています。
「依存の組み立てを 1 か所に集める」「各クラスは必要なものをコンストラクタで受け取る」。
これだけが DI の本質で、Hilt はこれを自動生成するものです(Step 4)。

## まだ残っている課題

- **書き込みのたびに `loadTodos()` を呼んでいる**: `addTodo` → `repository.addTodo()` → `loadTodos()`。呼び忘れると画面が古いまま。
- ViewModel の `Factory` と `AppContainer` が手書き。画面が増えると面倒。

## 確認方法

- 回転してもリストがちらつかないことを確認。
- 空文字で「追加」→ Toast が 1 回だけ出る(回転しても二重に出ない)。

次: [Step 3](step3.md) で Room と Flow を導入し、「読み直す」コードを消します。
