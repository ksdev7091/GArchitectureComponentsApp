# 解答例: Step 5

## 5-1 完了済みを隠すトグル、Compose 版

```kotlin
// TodoScreen.kt(状態あり版)
TodoScreen(
    uiState = uiState,
    onAdd = viewModel::addTodo,
    onDoneChange = viewModel::setDone,
    onDelete = viewModel::deleteTodo,
    onHideDoneChange = viewModel::setHideDone,   // 追加
    onErrorShown = viewModel::errorMessageShown,
)

// TodoScreen.kt(状態なし版)
@Composable
fun TodoScreen(
    uiState: TodoUiState,
    onAdd: (String) -> Unit,
    onDoneChange: (id: Long, isDone: Boolean) -> Unit,
    onDelete: (id: Long) -> Unit,
    onHideDoneChange: (Boolean) -> Unit,          // 追加
    onErrorShown: () -> Unit,
) {
    ...
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = uiState.hideDone, onCheckedChange = onHideDoneChange)
        Text("完了済みを隠す")
    }
    LazyColumn {
        items(uiState.visibleTodos, key = { it.id }) { todo -> ... }   // todos → visibleTodos
    }
}
```

ViewModel と `TodoViewModelTest` は 4-1 のまま。Preview には `onHideDoneChange = {}` を足します。

**4 回の実装の比較**

| | Step 0 | Step 2 | Step 3 | Step 5 |
|---|---|---|---|---|
| 変更ファイル | 2 | 3 | 3 | 1(UI のみ。VM は 4-1 で済み) |
| 状態の置き場 | Activity のリスト × 2 | UiState(`hideDone`) | `MutableStateFlow` + combine | 同左 |
| フィルタの同期 | 手動 `applyFilter()` | `render` が走る | `combine` が走る | 再コンポーズ |
| 回転対策 | 自前 `onSaveInstanceState` | 不要 | 不要 | 不要 |
| テスト | 不可 | 可(書いていない) | 可(書いていない) | 4-1 のテストがそのまま通る |
| UI 側の変更 | リスト 2 本の管理 | `submit(visibleTodos)` + CheckBox 同期 | 同左 | `items(visibleTodos)` + Switch |

Step 0 → 2 で「状態の置き場」が変わり、2 → 3 で「更新の仕方」が変わり、3 → 5 では**何も変わっていない**
(UI の描き方だけが変わった)ことが見えれば、この演習の目的は達成です。

## 5-2 Preview を 3 状態分つくる

```kotlin
private val previewCallbacks = object {
    val onAdd: (String) -> Unit = {}
    val onDoneChange: (Long, Boolean) -> Unit = { _, _ -> }
    val onDelete: (Long) -> Unit = {}
    val onErrorShown: () -> Unit = {}
}

@Preview(showBackground = true, name = "読み込み中")
@Composable
private fun TodoScreenLoadingPreview() {
    MaterialTheme { TodoScreen(uiState = TodoUiState(isLoading = true), /* callbacks */) }
}

@Preview(showBackground = true, name = "空")
@Composable
private fun TodoScreenEmptyPreview() {
    MaterialTheme { TodoScreen(uiState = TodoUiState(isLoading = false), /* callbacks */) }
}

@Preview(showBackground = true, name = "3 件(1 件完了)")
@Composable
private fun TodoScreenListPreview() {
    MaterialTheme {
        TodoScreen(
            uiState = TodoUiState(
                isLoading = false,
                todos = listOf(
                    Todo(1, "牛乳を買う", isDone = true, createdAt = 0),
                    Todo(2, "本を読む", isDone = false, createdAt = 0),
                    Todo(3, "散歩", isDone = false, createdAt = 0),
                ),
            ),
            /* callbacks */
        )
    }
}
```

状態あり版 `TodoScreen(viewModel)` には Preview を付けられません(Hilt と Room が必要になる)。
「ViewModel なしで見た目を確認したい」という要求が、状態なし版に分ける動機の 1 つです。

## 5-3 入力中のテキストを ViewModel に移すべきか(発展)

ViewModel に移した版:

```kotlin
@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    var inputText: String by savedStateHandle.saveable { mutableStateOf("") }
        private set

    fun onInputChange(value: String) { inputText = value }

    fun addTodo() {                       // 引数なしになる
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) { errorMessageRes.value = R.string.empty_title_error; return }
        viewModelScope.launch { repository.addTodo(trimmed) }
        inputText = ""
    }
}
```

**判断**: この画面では **UI 側(`rememberSaveable`)のままが適切**。

[docs/02-layers.md](../../../docs/02-layers.md) の UI 層ルール 6 の基準は「ViewModel の処理がその値を保持する必要があるか」。
この画面では入力文字列は追加ボタンのタイミングで一度渡すだけで、ViewModel は保持しません。
`rememberSaveable` で回転もプロセス再生成も生き延びるので、ViewModel に上げる理由がありません。
上げると、`addTodo` のテストが「`onInputChange` してから `addTodo()`」という 2 段階になり、
ViewModel の API が UI の都合(テキスト欄が 1 つある)に引きずられます。

一方 01-new-app の編集画面は、既存メモを読み込んでテキストに入れる・保存時に使う、と
ViewModel がテキストを扱う必要があるため、ViewModel 側(`savedStateHandle.saveable`)に置いています。
同じ「入力中のテキスト」でも、置き場所は画面の要件で決まります。
