# 解答例: Step 4

## 4-1 トグルのテストを書く

3-1 の実装を Step 4 にも入れたうえで:

```kotlin
@Test
fun `hideDone が true なら visibleTodos に完了済みが含まれない`() = runTest {
    val repository = FakeTodoRepository(listOf(todo(1, "a", isDone = true), todo(2, "b")))
    val viewModel = TodoViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    viewModel.setHideDone(true)

    assertEquals(listOf("b"), viewModel.uiState.value.visibleTodos.map { it.title })
}

@Test
fun `hideDone を切り替えても totalCount と doneCount は変わらない`() = runTest {
    val repository = FakeTodoRepository(listOf(todo(1, "a", isDone = true), todo(2, "b")))
    val viewModel = TodoViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    viewModel.setHideDone(true)

    assertEquals(2, viewModel.uiState.value.totalCount)
    assertEquals(1, viewModel.uiState.value.doneCount)
}
```

2 つ目のテストは「隠しても全件の数を出す」という仕様をコードとして固定しています。
将来誰かが `doneCount` を `visibleTodos` から数えるように書き換えたら、このテストが落ちます。

## 4-2 Fake に遅延を入れて isLoading をテストする

```kotlin
// FakeTodoRepository.kt
class FakeTodoRepository(initialTodos: List<Todo> = emptyList()) : TodoRepository {
    private val todos = MutableStateFlow(initialTodos)

    /** セットすると observeTodos() は最初の値を流す前にこれを待つ */
    var loadGate: CompletableDeferred<Unit>? = null

    override fun observeTodos(): Flow<List<Todo>> = flow {
        loadGate?.await()
        emitAll(todos)
    }
    ...
}
```

```kotlin
@Test
fun `Repository から最初の値が届くまでは isLoading が true`() = runTest {
    val repository = FakeTodoRepository(listOf(todo(1, "a"))).apply { loadGate = CompletableDeferred() }
    val viewModel = TodoViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

    assertTrue(viewModel.uiState.value.isLoading)

    repository.loadGate!!.complete(Unit)

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(1, viewModel.uiState.value.totalCount)
}
```

ポイント: `stateIn(initialValue = TodoUiState())` の `isLoading = true` が「最初の値が届くまで」の状態で、
`combine` が一度でも走れば `isLoading = false` になります。Fake にゲートを付けると、その「届くまで」を
テストから止めて観察できます。

## 4-3 Hilt の対応表を 1 行変えると何が起きるか

```kotlin
// DataModule.kt(実験用)
@Binds
abstract fun bindTodoRepository(impl: FakeTodoRepository): TodoRepository
```

`FakeTodoRepository` を `src/main` に移し、`@Inject constructor()` を付ければビルドが通り、
起動時は空、追加すれば増え、**アプリを終了すると消える** Todo アプリになります。
`MainActivity` / `TodoViewModel` / `TodoScreen` は 1 文字も変えていません。

これが依存性注入の「対応表を 1 か所変えるだけで、実装を丸ごと差し替えられる」という性質です。
01-new-app では同じ仕組みを flavor(`src/demo` と `src/prod` の `RemoteModule`)で使い、
疑似リモートと Retrofit の本物のネットワークを切り替えています。
テストでは `@TestInstallIn` で同じことをテスト専用モジュールから行えます。
