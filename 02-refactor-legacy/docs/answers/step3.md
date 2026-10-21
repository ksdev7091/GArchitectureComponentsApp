# 解答例: Step 3

## 3-1 完了済みを隠すトグル、Flow 版

```kotlin
// TodoViewModel.kt
class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    private val hideDone = MutableStateFlow(false)
    private val errorMessageRes = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<TodoUiState> =
        combine(repository.observeTodos(), hideDone, errorMessageRes) { todos, hide, errorRes ->
            TodoUiState(todos = todos, hideDone = hide, isLoading = false, errorMessageRes = errorRes)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())

    fun setHideDone(hide: Boolean) {
        hideDone.value = hide
    }
    ...
}
```

`TodoUiState.visibleTodos` と `MainActivity` の変更は 2-1 と同じです。

**2-1 との違い**
- 2-1 では `_uiState.update { it.copy(hideDone = hide) }` と、ViewModel が UiState を**自分で書き換えて**いた。
- 3-1 では `hideDone` という入力を変えるだけで、UiState は `combine` が**導出**する。
  ViewModel は「UiState がどう作られるか」を宣言しているだけで、「いつ更新するか」を管理していない。
- 入力が増えたら `combine` の引数を増やすだけ。`loadTodos()` の呼び忘れという種類のバグが構造的に起きない。

これが「状態を所有する ViewModel」から「状態を導出する ViewModel」への変化です。
Repository の Flow(Room)、ユーザー入力(hideDone)、UI 都合のフラグ(errorMessageRes)という
3 種類の出どころを 1 つの UiState に合成している点にも注目してください。

## 3-2 件数を DB で数えるか、UiState で数えるか

```kotlin
// TodoDao.kt
@Query("SELECT COUNT(*) FROM todos WHERE is_done = 1")
fun observeDoneCount(): Flow<Int>

// TodoRepository.kt に fun observeDoneCount(): Flow<Int> を追加し、
// TodoViewModel の combine に 1 つ増やして TodoUiState(doneCount = count, ...) とする
```

**判断**: この規模では**UiState で計算する(元の設計)ほうが良い**。

| 観点 | UiState で計算 | DB で数える |
|---|---|---|
| 二重管理 | なし(`todos` から導く) | `todos` と `doneCount` が別のクエリで来る。一瞬ずれる(一覧は更新されたが件数は前のまま)可能性がある |
| 10 万行のとき | 全件をメモリに持つ時点で一覧表示側が先に破綻する。一覧をページングするなら、件数は DB で数える設計に変わる | 件数だけなら軽い |
| テスト | `TodoUiState(todos).doneCount` を直接検証できる | Fake Repository に件数の Flow も持たせる必要がある |

つまり「一覧を全件メモリに持つ前提なら UiState で計算、一覧をページングする規模なら DB で数える」。
設計は規模で変わる、という例です。

## 3-3 Flow が本当に「勝手に流れてくる」ことを確かめる

Database Inspector で行を INSERT すると、**Step 3 では画面が即座に更新されます**。
Room が `todos` テーブルへの書き込みを検知し、`observeAll()` の Flow が新しいリストを流し、
`combine` → `stateIn` → `collect` → `render` と伝わるからです。
アプリ側のコードは何もしていません。

**Step 2 では更新されません。** `loadTodos()` を誰かが呼ぶまで、ViewModel は古いリストを持ち続けます。
画面を回転しても ViewModel は生き残るので、古いままです。アプリを再起動して初めて反映されます。

「書いたら読み直す」をプログラマが管理する(Step 2)と、変更を Room が通知する(Step 3)の違いが、ここで目に見えます。
