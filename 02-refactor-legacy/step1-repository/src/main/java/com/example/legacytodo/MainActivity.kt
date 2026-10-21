package com.example.legacytodo

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
import androidx.lifecycle.lifecycleScope
import com.example.legacytodo.data.SqliteTodoRepository
import com.example.legacytodo.data.TodoDbHelper
import com.example.legacytodo.data.TodoRepository
import com.example.legacytodo.model.Todo
import kotlinx.coroutines.launch

/**
 * 【Step 1: Data 層を切り出す】
 *
 * Step 0 からの変化:
 * - DB アクセスのコードが Activity から消え、[TodoRepository] を呼ぶだけになった(①の一部を解決)
 * - `Thread` + `runOnUiThread` が `lifecycleScope.launch` + suspend 関数になった(②を解決)
 *   lifecycleScope は Activity の destroy 時に自動でキャンセルされるので、
 *   destroy 後の Activity を掴んだまま処理が走り続けることがなくなった。
 *
 * まだ残っている問題:
 * - ① Adapter はまだ Activity の inner class で、Activity のフィールドを直接触っている。
 * - ③ 状態(todos)はまだ Activity にある。回転すると読み直す。
 * - ④ 完了数の集計(業務ロジック)はまだ Activity にある。
 * - ⑤ Repository を Activity 自身が組み立てている(`SqliteTodoRepository(TodoDbHelper(...))`)。
 *   そのため DbHelper の寿命管理(onDestroy での close)もまだ Activity の仕事。
 * - 書き込みのたびに `loadTodos()` を手で呼ぶ必要がある。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: TodoDbHelper // ⑤ まだ Activity が組み立て、寿命も管理する
    private lateinit var repository: TodoRepository
    private lateinit var summaryText: TextView
    private lateinit var input: EditText
    private lateinit var adapter: TodoAdapter

    private val todos = ArrayList<Todo>() // ③ まだ Activity が状態を持つ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TodoDbHelper(applicationContext)
        repository = SqliteTodoRepository(dbHelper)

        summaryText = findViewById(R.id.summary_text)
        input = findViewById(R.id.input)
        val listView = findViewById<ListView>(R.id.todo_list)
        adapter = TodoAdapter()
        listView.adapter = adapter

        findViewById<Button>(R.id.add_button).setOnClickListener { addTodo() }

        loadTodos()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close() // Step 2 で Repository がアプリ全体で 1 つになると、この行は不要になる
    }

    // Repository を呼ぶだけ。スレッドのことは考えなくてよい
    private fun loadTodos() {
        lifecycleScope.launch {
            val result = repository.getTodos()
            todos.clear()
            todos.addAll(result)
            adapter.notifyDataSetChanged()
            updateSummary()
        }
    }

    private fun addTodo() {
        val title = input.text.toString().trim()
        if (title.isEmpty()) { // ④ 入力チェックはまだここ
            Toast.makeText(this, R.string.empty_title_error, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            repository.addTodo(title)
            input.text.clear()
            loadTodos() // まだ手動で読み直している
        }
    }

    private fun setDone(todo: Todo, isDone: Boolean) {
        lifecycleScope.launch {
            repository.setDone(todo.id, isDone)
            loadTodos()
        }
    }

    private fun deleteTodo(todo: Todo) {
        lifecycleScope.launch {
            repository.deleteTodo(todo.id)
            loadTodos()
        }
    }

    // ④ 業務ロジックはまだ UI にある
    private fun updateSummary() {
        val doneCount = todos.count { it.isDone }
        summaryText.text = getString(R.string.summary_format, todos.size, doneCount)
    }

    // ① Adapter はまだ inner class のまま(Step 2 で分離する)
    private inner class TodoAdapter : BaseAdapter() {
        override fun getCount(): Int = todos.size
        override fun getItem(position: Int): Todo = todos[position]
        override fun getItemId(position: Int): Long = todos[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_todo, parent, false)
            val todo = todos[position]

            val check = view.findViewById<CheckBox>(R.id.check)
            check.setOnCheckedChangeListener(null)
            check.text = todo.title
            check.isChecked = todo.isDone
            check.setOnCheckedChangeListener { _, isChecked -> setDone(todo, isChecked) }

            view.findViewById<Button>(R.id.delete_button).setOnClickListener { deleteTodo(todo) }
            return view
        }
    }
}
