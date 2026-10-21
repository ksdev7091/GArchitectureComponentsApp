# 解答例: Step 1

## 1-1 FakeTodoRepository を書く

```kotlin
// src/test/java/com/example/legacytodo/testing/FakeTodoRepository.kt
class FakeTodoRepository(
    initialTodos: List<Todo> = emptyList(),
) : TodoRepository {

    private val todos = initialTodos.toMutableList()
    private var nextId = (initialTodos.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun getTodos(): List<Todo> = todos.sortedByDescending { it.createdAt }

    override suspend fun addTodo(title: String) {
        todos += Todo(id = nextId++, title = title, isDone = false, createdAt = todos.size.toLong())
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        val index = todos.indexOfFirst { it.id == id }
        if (index >= 0) todos[index] = todos[index].copy(isDone = isDone)
    }

    override suspend fun deleteTodo(id: Long) {
        todos.removeAll { it.id == id }
    }
}
```

Step 4 の版との違いは、`getTodos(): List` が `observeTodos(): Flow` になっている点だけです
(Step 3 で Repository の API が Flow に変わるため)。

**Step 0 で同じものが書けない理由**: 差し替える対象(インターフェース)が存在しないから。
`MainActivity` は `TodoDbHelper` という具象クラスを自分で `new` しているので、
Fake を作っても渡す場所がありません。Step 1 でインターフェースを切ったことで、初めて「差し込む穴」ができました。

## 1-2 DB の知識が Activity から消えたか

Step 1 の `MainActivity.kt` に `ContentValues` / `SQLite` / `Cursor` の import はありません。
唯一残っているのは `TodoDbHelper` の生成と `close()` で、これは「Repository の組み立てと寿命管理」であって DB アクセスではありません。
その責任も Step 2 の `AppContainer` に移ります。

## 1-3 ioDispatcher の継ぎ目を使う

```kotlin
@Test
fun `getTodos を JVM で呼ぶ`() = runTest {
    val repository = SqliteTodoRepository(
        dbHelper = TodoDbHelper(/* Context が必要 */),   // ← ここで詰まる
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )
    repository.getTodos()
}
```

`TodoDbHelper` は `Context` を要求し、`SQLiteOpenHelper` は Android の SQLite 実装に依存するので、
JVM では `RuntimeException: Method ... not mocked` になります。ディスパッチャを差し替えられても、
**DB そのものが JVM で動かない**ので Repository のテストは書けません。

Step 3 で Room にすると、
- Repository は `TodoDao`(インターフェース)に依存するようになり、DAO の Fake を渡せば JVM でテストできる
- Room の suspend 関数が main-safe なので、`ioDispatcher` 引数そのものが不要になる

という 2 つの変化が起きます。`ioDispatcher` は「Repository の中で自前のスレッド切り替えをしている間だけ必要な継ぎ目」です。
