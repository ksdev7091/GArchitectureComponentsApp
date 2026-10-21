# Step 3: Room + Flow — 「読み直す」をやめる

モジュール: `step3-room-flow`(比較対象: `step2-viewmodel`)

## 前のステップの課題

- 書き込みのたびに `loadTodos()` を呼んで読み直している。忘れると画面が古いままになる
- SQL が文字列で、列名のタイプミスが実行時までわからない
- `Cursor` の読み取りコードが冗長

## 何を変えたか

```
step3-room-flow/src/main/java/com/example/legacytodo/
├── MainActivity.kt               ★無変更(Step 2 のファイルをそのままコピー。Compare Files で差分ゼロ)
├── TodoApplication.kt            AppContainer が Room を組み立てるように
├── model/Todo.kt
├── data/
│   ├── TodoRepository.kt         getTodos(): List → observeTodos(): Flow
│   ├── RoomTodoRepository.kt     ★新設(SqliteTodoRepository と TodoDbHelper は削除)
│   └── local/
│       ├── TodoEntity.kt         ★新設: @Entity
│       ├── TodoDao.kt            ★新設: @Dao, observeAll(): Flow
│       └── TodoDatabase.kt       ★新設
└── ui/
    ├── TodoViewModel.kt          loadTodos() が消え、Flow を変換するだけに
    └── TodoAdapter.kt            (無変更)
```

1. **`TodoRepository.getTodos(): List<Todo>` → `observeTodos(): Flow<List<Todo>>`**
   「取りに行く」から「流れてくる」へ。
2. **Room を導入**: `TodoEntity` / `TodoDao` / `TodoDatabase`。`TodoDao.observeAll()` は `Flow` を返す。
3. **`RoomTodoRepository`**: DAO を呼び、`TodoEntity` → `Todo` に変換するだけ。`withContext` も消えた。
4. **`TodoViewModel`**: `_uiState` を自分で書き換えるのをやめ、`repository.observeTodos()` を `combine` + `stateIn` で `StateFlow<TodoUiState>` に変換するだけになった。`loadTodos()` は消滅。

## なぜこれで解決するのか

### Room の Flow は「テーブルが変わったら再クエリして流す」

`@Query` の戻り値を `Flow` にすると、Room はそのテーブルへの書き込みを監視し、変更があるたびに自動で再クエリして新しいリストを流します。
`addTodo` で INSERT すれば、`observeAll()` の Flow が勝手に新しいリストを流し、それが `combine` → `stateIn` → UI へ届く。
**「書いたら読み直す」を人間が覚えている必要がなくなった**。これが Step 2 の最大の課題の解決です。

### ViewModel は「状態を所有する」から「状態を導出する」へ

Step 2 の ViewModel:
```kotlin
private val _uiState = MutableStateFlow(TodoUiState())   // 自分で持つ
fun addTodo(...) { ...; repository.addTodo(); loadTodos() }  // 自分で更新する
```
Step 3 の ViewModel:
```kotlin
val uiState = combine(repository.observeTodos(), errorMessageRes) { ... }.stateIn(...)  // 導出するだけ
fun addTodo(...) { ...; viewModelScope.launch { repository.addTodo(trimmed) } }  // 書くだけ
```
ViewModel が自分で書き換える状態は、UI 側の都合で生まれる `errorMessageRes` だけになりました。
「状態の真実は Data 層(Room)にあり、ViewModel はそれを UI 向けに変換している」という推奨アーキテクチャの形です(SSOT)。

### `stateIn(WhileSubscribed(5_000))`

Flow を `StateFlow` に変換するとき、「誰も購読していないときは上流(Room のクエリ)を止める」設定。
5 秒の猶予があるので、画面回転のような短い中断では止まらず、再購読時に即座に最新値が返ります。

### Repository のインターフェースを変えたのに Activity が無変更な理由

Activity は `TodoViewModel.uiState` しか見ていません。Repository の API が `List` から `Flow` に変わった影響は ViewModel で吸収されています。
層を分けると「変更が波及する範囲」が 1 層先で止まる。Step 1 でインターフェースを切った見返りがここで出ました。

### Entity とモデルを分ける

`TodoEntity`(DB の行。`@ColumnInfo(name = "is_done")` のような DB の都合を持つ)と `Todo`(アプリのモデル)を分け、Repository で変換しています。
DB のカラム名を変えても、UI 層と ViewModel は影響を受けません。

## 実務での注意: 既存 DB の移行

このリポジトリでは各ステップが別アプリ(`applicationId` が違う)なので、Step 3 は Step 2 の `todo.db` とは無関係に `todo_room.db` を新規作成しています。
本番アプリで同じことをする場合、Room は既存テーブルのスキーマを検証するため、同じ DB ファイルを引き継ぐなら `Migration` を書くか、旧テーブルから新テーブルへデータをコピーする処理が必要です。

## まだ残っている課題

- `AppContainer` と `TodoViewModel.Factory` が手書き
- テストがまだない

次: [Step 4](step4.md) で Hilt を導入し、単体テストを書きます。
