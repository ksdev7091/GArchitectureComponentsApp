# Step 1: Data 層を切り出す

モジュール: `step1-repository`(比較対象: `step0-legacy`)

## 前のステップの課題(このステップで解決するもの)

- ① DB アクセスが Activity に直書きされている
- ② 生の `Thread` + `runOnUiThread`。破棄済みの Activity を掴んだまま処理が走り続ける

## 何を変えたか

```
step1-repository/src/main/java/com/example/legacytodo/
├── MainActivity.kt                 DB コードが消え、repository を呼ぶだけに
├── model/Todo.kt                   (移動のみ)
└── data/
    ├── TodoRepository.kt           ★新設: インターフェース(suspend 関数 4 つ)
    ├── SqliteTodoRepository.kt     ★新設: Step 0 の DB コードをほぼそのまま移した実装
    └── TodoDbHelper.kt             (移動のみ)
```

1. **`TodoRepository` インターフェースを定義した**
   `getTodos()` / `addTodo()` / `setDone()` / `deleteTodo()`。すべて `suspend`。
2. **`SqliteTodoRepository` に Step 0 の DB コードを移した**
   SQL も `ContentValues` もそのまま。`Thread { }` を `withContext(Dispatchers.IO) { }` に置き換えただけ。
3. **MainActivity は `lifecycleScope.launch { repository.getTodos() }` と呼ぶだけになった**

Compare Files で `MainActivity.kt` を見比べると、`loadTodos()` が 25 行から 9 行になっています。

`SqliteTodoRepository` のコンストラクタにある `ioDispatcher` 引数は、テストで `Dispatchers.IO` を差し替えるための「継ぎ目」です。
このキットでは使いませんが、Repository を JVM でテストするときの定石として置いてあります(Room 化する Step 3 で不要になります)。

## なぜこれで解決するのか

### 「どこに保存されているか」を UI から隠す

Activity は `TodoRepository` というインターフェースしか知りません。
中身が SQLite なのか Room なのかサーバーなのかは、Activity には関係ない。
これが **関心の分離** の最初の一歩で、Step 3 で Room に差し替えるときに Activity を触らずに済む理由です。

### main-safe: スレッドのことは Data 層が責任を持つ

Step 0 では Activity が「DB は別スレッドで、結果はメインスレッドで」と毎回書いていました。
Step 1 では Repository の中で `withContext(Dispatchers.IO)` をやるので、
**呼び出し側はメインスレッドから普通に呼ぶだけ**です。これを main-safe と呼びます。

### lifecycleScope: Activity が消えたら続きの処理は止まる

`lifecycleScope.launch` で起動したコルーチンは、Activity の `onDestroy` で自動的にキャンセルされます。
Step 0 の「destroy 後に古い Activity の画面を更新しようとする」問題はこれで消えました。
ただし `withContext(IO)` の中で実行中の DB 書き込みが途中で巻き戻されるわけではありません。
キャンセルされるのは「書き込みが終わったあとの続き(UI 更新)」です。

### DbHelper の寿命はまだ Activity が管理している

`TodoDbHelper` は Activity ごとに作られ、`onDestroy` で `close()` しています。Step 0 と同じく、
close と実行中の DB アクセスが競合する可能性は残っています。
Step 2 で Repository をアプリ全体で 1 つにすると、DbHelper もアプリの寿命と一致し、この問題は消えます。

### インターフェースにした理由

今の時点では実装は 1 つしかないので、インターフェースは「過剰」に見えるかもしれません。
しかし、インターフェースがあることで:
- テストで `FakeTodoRepository` を差し込める(Step 4 で実際に書く)
- Room 版の実装に差し替えられる(Step 3)
インターフェースは「後から差し替えるための継ぎ目」です。

## まだ残っている課題

- ① Adapter はまだ Activity の inner class で、Activity のフィールドを直接触っている
- ③ `todos` は Activity のフィールドのまま。回転するとまた読み直す
- ④ 完了数の集計と空文字チェックは Activity のまま
- ⑤ `SqliteTodoRepository(TodoDbHelper(applicationContext))` と Activity が組み立て、DbHelper の close も Activity がやっている
- 書き込みのたびに `loadTodos()` を呼んでいる

## 確認方法

Step 0 と同じ操作をして、同じように動くことを確かめてください。
見た目の変化はゼロです。「見た目を変えずに内部構造だけ変える」のがリファクタリングです。

次: [Step 2](step2.md) で状態と業務ロジックを ViewModel に移します。
