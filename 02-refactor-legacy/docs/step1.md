# Step 1: Data 層を切り出す

モジュール: `step1-repository`(比較対象: `step0-legacy`)

## 前のステップの課題

- ① DB アクセスが Activity に直書きされている
- ② 生の `Thread` + `runOnUiThread`。Activity 破棄後のクラッシュ、エラー処理なし
- ⑤ `TodoDbHelper` を Activity が `new` している

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

Compare Files で `MainActivity.kt` を見比べると、`loadTodos()` が 25 行から 8 行になっています。

## なぜこれで解決するのか

### 「どこに保存されているか」を UI から隠す

Activity は `TodoRepository` というインターフェースしか知りません。
中身が SQLite なのか Room なのかサーバーなのかは、Activity には関係ない。
これが **関心の分離** の最初の一歩で、Step 3 で Room に差し替えるときに Activity を触らずに済む理由です。

### main-safe: スレッドのことは Data 層が責任を持つ

Step 0 では Activity が「DB は別スレッドで、結果はメインスレッドで」と毎回書いていました。
Step 1 では Repository の中で `withContext(Dispatchers.IO)` をやるので、
**呼び出し側はメインスレッドから普通に呼ぶだけ**です。これを main-safe と呼びます。

### lifecycleScope: Activity が消えたら処理も止まる

`lifecycleScope.launch` で起動したコルーチンは、Activity の `onDestroy` で自動的にキャンセルされます。
Step 0 の「destroy 後に `runOnUiThread` が走る」問題はこれで消えました。

### インターフェースにした理由

今の時点では実装は 1 つしかないので、インターフェースは「過剰」に見えるかもしれません。
しかし、インターフェースがあることで:
- テストで `FakeTodoRepository` を差し込める(Step 4 で実際に書く)
- Room 版の実装に差し替えられる(Step 3)
インターフェースは「後から差し替えるための継ぎ目」です。

## まだ残っている課題

- ③ `todos` は Activity のフィールドのまま。回転するとまた読み直す
- ④ 完了数の集計と空文字チェックは Activity のまま
- ⑤ `SqliteTodoRepository(TodoDbHelper(applicationContext))` と Activity が組み立てている
- 書き込みのたびに `loadTodos()` を呼んでいる

## 確認方法

Step 0 と同じ操作をして、同じように動くことを確かめてください。
見た目の変化はゼロです。「見た目を変えずに内部構造だけ変える」のがリファクタリングです。

次: [Step 2](step2.md) で状態と業務ロジックを ViewModel に移します。
