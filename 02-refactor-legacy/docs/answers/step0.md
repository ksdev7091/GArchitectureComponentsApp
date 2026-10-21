# 解答例: Step 0

解答は「抜粋」です。そのまま貼るのではなく、自分の実装と比べて何が違うかを見てください。

## 0-1 完了済みを隠すトグル

変更ファイル: `activity_main.xml`、`MainActivity.kt`(2 ファイル、追加・変更は約 25 行)。

```xml
<!-- activity_main.xml: summary_text の下に追加 -->
<CheckBox
    android:id="@+id/hide_done"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="完了済みを隠す" />
```

```kotlin
// MainActivity.kt
private lateinit var hideDoneCheck: CheckBox
private val todos = ArrayList<Todo>()          // 全件(集計用)
private val visibleTodos = ArrayList<Todo>()   // 表示用(フィルタ後)  ← 状態が 2 つに増えた

override fun onCreate(savedInstanceState: Bundle?) {
    ...
    hideDoneCheck = findViewById(R.id.hide_done)
    hideDoneCheck.setOnCheckedChangeListener { _, _ -> applyFilter() }
    ...
}

private fun applyFilter() {
    visibleTodos.clear()
    visibleTodos.addAll(if (hideDoneCheck.isChecked) todos.filter { !it.isDone } else todos)
    adapter.notifyDataSetChanged()
}

// loadTodos() の runOnUiThread 内: todos を入れ替えたあとに applyFilter() を呼ぶ
// updateSummary() は todos(全件)を使うので変更なし
// TodoAdapter の getCount/getItem/getItemId に加えて、getView の `val todo = todos[position]` も
// visibleTodos を参照するように変更(忘れると、隠したあとに行の内容がずれる)
```

**観察してほしいこと**
- 「全件」と「表示用」の 2 つのリストを Activity が手で同期している。`applyFilter()` の呼び忘れがそのままバグになる。
- 回転: `CheckBox` は id があれば View の状態として自動で復元され、復元時にリスナーも発火する。
  ただし復元は `onCreate` 直後で、`loadTodos()` の完了(別スレッド)より前なので、
  `applyFilter()` が空の `todos` に対して走り、そのあと `loadTodos()` の完了時にもう一度 `applyFilter()` が必要になる。
  「どのタイミングで何が復元され、いつデータが揃うか」を Activity のライフサイクルに沿って自分で追いかける必要がある。
- このロジックが正しいかを確かめる方法は、エミュレータでチェックを入れて目で見るしかない。

## 0-2 集計のバグをテストで捕まえられるか

**書けません。** 理由は `updateSummary()` が
1. `private` で、
2. `MainActivity` のインスタンスメソッドで、
3. `todos`(Activity のフィールド)と `summaryText`(View)に依存している

からです。`MainActivity` を JVM で `new` することはできず(Android フレームワークが生成する)、
仮にできたとしても `TextView` は JVM 上では動きません。

テストしたいのは `todos.count { it.isDone }` という 1 行の計算なのに、
その 1 行が Activity・View・DB と絡み合っているために取り出せない。これが「関心の分離ができていない」ことの具体的な意味です。
Step 2 でこの計算は `TodoUiState.doneCount` に移り、Step 4 で実際にテストできるようになります。

## 0-3 回転中のクラッシュを再現する

**`val db = dbHelper.readableDatabase` の直後に sleep を入れた場合**: 回転中に
- 古い Activity の `onDestroy` → `dbHelper.close()` が走り、取得済みの `db` が閉じられる
- 3 秒後、Thread が `db.query(...)` を呼び、
  `IllegalStateException: attempt to re-open an already-closed object: SQLiteDatabase ...todo.db` でクラッシュする

**`Thread { ... }` の先頭に sleep を入れた場合**: クラッシュしません。
`SQLiteOpenHelper.close()` は内部の参照を捨てるだけなので、3 秒後の `dbHelper.readableDatabase` は DB を**開き直し**、
クエリは成功し、`runOnUiThread` は破棄済みの古い Activity の `adapter` / `summaryText` を黙って更新します
(新しい Activity には反映されない。古い Activity は Thread に掴まれたままメモリに残る = リーク)。

つまり、生の Thread は「どこで止まっているか」によってクラッシュにもリークにもなり、Activity の寿命を一切知りません。
これがコメント②の意味です。Step 1 の `lifecycleScope` が「続きの処理」をキャンセルし、Step 2 で DbHelper をアプリの寿命に合わせることで、両方が解消します。
