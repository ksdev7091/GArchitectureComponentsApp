package com.example.legacytodo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import com.example.legacytodo.R
import com.example.legacytodo.model.Todo

class TodoAdapter(
    private val onDoneChange: (id: Long, isDone: Boolean) -> Unit,
    private val onDelete: (id: Long) -> Unit,
) : BaseAdapter() {

    private var items: List<Todo> = emptyList()

    fun submit(todos: List<Todo>) {
        items = todos
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): Todo = items[position]
    override fun getItemId(position: Int): Long = items[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        val todo = items[position]

        val check = view.findViewById<CheckBox>(R.id.check)
        check.setOnCheckedChangeListener(null)
        check.text = todo.title
        check.isChecked = todo.isDone
        check.setOnCheckedChangeListener { _, isChecked -> onDoneChange(todo.id, isChecked) }

        view.findViewById<Button>(R.id.delete_button).setOnClickListener { onDelete(todo.id) }
        return view
    }
}
