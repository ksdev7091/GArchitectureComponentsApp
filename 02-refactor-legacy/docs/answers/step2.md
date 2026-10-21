# 解答例: Step 2

## 2-1 完了済みを隠すトグル、ViewModel 版

変更ファイル: `activity_main.xml`(CheckBox 追加)、`TodoViewModel.kt`、`MainActivity.kt`。

```kotlin
// TodoViewModel.kt
data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val hideDone: Boolean = false,
    val isLoading: Boolean = true,
    @StringRes val errorMessageRes: Int? = null,
) {
    val totalCount: Int get() = todos.size
    val doneCount: Int get() = todos.count { it.isDone }
    /** 画面に出す Todo。元の todos は保持したまま、表示用は計算で導く(二重管理しない) */
    val visibleTodos: List<Todo> get() = if (hideDone) todos.filter { !it.isDone } else todos
}

class TodoViewModel(...) : ViewModel() {
    ...
    fun setHideDone(hide: Boolean) {
        _uiState.update { it.copy(hideDone = hide) }
    }
}
```

```kotlin
// MainActivity.kt
findViewById<CheckBox>(R.id.hide_done).setOnCheckedChangeListener { _, checked ->
    viewModel.setHideDone(checked)
}

private fun render(state: TodoUiState) {
    adapter.submit(state.visibleTodos)   // ← todos から visibleTodos に変えるだけ
    hideDoneCheck.isChecked = state.hideDone // 回転後も状態から復元される
    ...
}
```

**0-1 との比較**
| | Step 0 | Step 2 |
|---|---|---|
| 状態の置き場 | Activity に `todos` と `visibleTodos` の 2 つ | UiState に `hideDone` 1 つ。表示用は計算 |
| フィルタの同期 | `applyFilter()` を手で呼ぶ | 不要(UiState が変われば `render` が走る) |
| 回転対策 | `onSaveInstanceState` を自前実装 | 不要(ViewModel が生き残る) |
| テスト | 不可 | `TodoUiState(todos, hideDone = true).visibleTodos` を JVM で検証できる |

注意点: `render()` で `hideDoneCheck.isChecked = state.hideDone` を設定するとリスナーが発火し、
`setHideDone` が再度呼ばれます。値が同じなら `update` は同じ状態を作るだけで無限ループにはなりませんが、
気になるなら `if (hideDoneCheck.isChecked != state.hideDone)` で囲みます。
(Compose では `Checkbox(checked = ..., onCheckedChange = ...)` が最初からこの形なので、この問題が起きません)

## 2-2 「読み込み中」の表示を確かめる

`delay(2000)` を入れると起動後 2 秒間「読み込み中…」が出て、そのあと件数に変わります。
`render()` から `isLoading` の参照を消すと、2 秒間「0 件中 0 件完了」と表示されます。
これは**嘘の情報**です。データがまだ無いことと、データが 0 件であることは違います。

状態に `isLoading` を入れたなら、画面はそれを必ず描かなければなりません。
描かない状態は「存在しないのと同じ」で、UDF の「画面は状態をそのまま映す」が破れています。
逆に、描くつもりがない状態は UiState に入れないほうがよい(読む人を混乱させる)。

## 2-3 ViewModel を JVM だけで new する

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `完了数が集計される`() = runTest {
        val repository = FakeTodoRepository(
            listOf(
                Todo(id = 1, title = "a", isDone = true, createdAt = 0),
                Todo(id = 2, title = "b", isDone = false, createdAt = 0),
            ),
        )

        val viewModel = TodoViewModel(repository)

        assertEquals(1, viewModel.uiState.value.doneCount)
    }
}
```

**MainDispatcherRule が必要な理由**: `TodoViewModel` の `init` で `viewModelScope.launch { ... }` を呼んでおり、
`viewModelScope` は `Dispatchers.Main` を使います。JVM にはメインスレッド(Android の Looper)がないので、
差し替えないと `IllegalStateException: Module with the Main dispatcher had failed to initialize` になります。
`UnconfinedTestDispatcher` に差し替えると、`launch` の中身がその場で同期的に実行され、
`init` の `loadTodos()` が終わった状態で `uiState.value` を読めます。

Step 2 の時点でこのテストが書けることに注目してください。Step 4 で Hilt を入れるのはテストのためではなく、組み立ての自動化のためです。
