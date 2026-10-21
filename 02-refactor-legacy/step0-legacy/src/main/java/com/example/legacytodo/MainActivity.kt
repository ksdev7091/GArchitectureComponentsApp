package com.example.legacytodo

import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 【Step 0: リファクタリング前の "よくある" Activity】
 *
 * 一見ちゃんと動く。しかし次の問題を抱えている(番号はコード中のコメントと対応):
 *
 * ① Activity が「画面」「DB アクセス」「スレッド管理」「業務ロジック」をすべて抱えている。
 *    → 1 ファイルが肥大化し、どこを直せば何が変わるのか読み解けなくなる。
 * ② 生の Thread + runOnUiThread。Activity が destroy された後に runOnUiThread が走る可能性がある。
 *    → 画面回転中にクラッシュ/リーク。エラー処理もない。
 * ③ 状態(todos)が Activity のフィールドにある。画面回転のたびに Activity が作り直され、
 *    毎回 DB から読み直す。読み直し中は一瞬リストが空になる。
 * ④ 「何件中何件完了」のような業務ロジックが UI コードに埋まっている。
 * ⑤ 依存(TodoDbHelper)を Activity 自身が new している。テストで差し替える手段がない。
 * ⑥ 以上の結果、このロジックを自動テストする方法が「エミュレータで手動操作」しかない。
 *
 * これらは「関心の分離」ができていないことに起因する。Step 1 以降で 1 つずつほどいていく。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: TodoDbHelper // ⑤ 自分で new する依存
    private lateinit var summaryText: TextView
    private lateinit var input: EditText
    private lateinit var adapter: TodoAdapter

    private val todos = ArrayList<Todo>() // ③ 画面の状態が Activity に直接ある

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TodoDbHelper(this)

        summaryText = findViewById(R.id.summary_text)
        input = findViewById(R.id.input)
        val listView = findViewById<ListView>(R.id.todo_list)
        adapter = TodoAdapter()
        listView.adapter = adapter

        findViewById<Button>(R.id.add_button).setOnClickListener { addTodo() }

        loadTodos() // ③ 回転のたびに DB から全件読み直す
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    // ① DB アクセスが Activity に直接書かれている
    private fun loadTodos() {
        Thread { // ② 生のスレッド
            val db = dbHelper.readableDatabase
            val cursor = db.query("todos", null, null, null, null, null, "created_at DESC")
            val result = ArrayList<Todo>()
            cursor.use {
                while (it.moveToNext()) {
                    result.add(
                        Todo(
                            id = it.getLong(it.getColumnIndexOrThrow("id")),
                            title = it.getString(it.getColumnIndexOrThrow("title")),
                            isDone = it.getInt(it.getColumnIndexOrThrow("is_done")) == 1,
                            createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        ),
                    )
                }
            }
            runOnUiThread { // ② Activity が既に destroy されていても呼ばれる
                todos.clear()
                todos.addAll(result)
                adapter.notifyDataSetChanged()
                updateSummary()
            }
        }.start()
    }

    private fun addTodo() {
        val title = input.text.toString().trim()
        if (title.isEmpty()) { // ④ 入力チェック(業務ルール)が UI に埋まっている
            Toast.makeText(this, R.string.empty_title_error, Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val values = ContentValues().apply {
                put("title", title)
                put("is_done", 0)
                put("created_at", System.currentTimeMillis())
            }
            dbHelper.writableDatabase.insert("todos", null, values)
            runOnUiThread {
                input.text.clear()
                loadTodos() // 書き込んだら手動で読み直す。忘れると画面が古いまま
            }
        }.start()
    }

    private fun setDone(todo: Todo, isDone: Boolean) {
        Thread {
            val values = ContentValues().apply { put("is_done", if (isDone) 1 else 0) }
            dbHelper.writableDatabase.update("todos", values, "id = ?", arrayOf(todo.id.toString()))
            runOnUiThread { loadTodos() }
        }.start()
    }

    private fun deleteTodo(todo: Todo) {
        Thread {
            dbHelper.writableDatabase.delete("todos", "id = ?", arrayOf(todo.id.toString()))
            runOnUiThread { loadTodos() }
        }.start()
    }

    // ④ 業務ロジック(完了数の集計)が UI に埋まっている。テストするには画面を起動するしかない
    private fun updateSummary() {
        val doneCount = todos.count { it.isDone }
        summaryText.text = getString(R.string.summary_format, todos.size, doneCount)
    }

    // ① Adapter まで Activity の inner class。Activity のフィールドに直接触っている
    private inner class TodoAdapter : BaseAdapter() {
        override fun getCount(): Int = todos.size
        override fun getItem(position: Int): Todo = todos[position]
        override fun getItemId(position: Int): Long = todos[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_todo, parent, false)
            val todo = todos[position]

            val check = view.findViewById<CheckBox>(R.id.check)
            check.setOnCheckedChangeListener(null) // リサイクル時に前のリスナーが発火しないように
            check.text = todo.title
            check.isChecked = todo.isDone
            check.setOnCheckedChangeListener { _, isChecked -> setDone(todo, isChecked) }

            view.findViewById<Button>(R.id.delete_button).setOnClickListener { deleteTodo(todo) }
            return view
        }
    }
}
