package com.example.todoappfullkotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.util.*

data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var isCompleted: Boolean = false,
    var order: Int = 0
)

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var deleteAllBtn: Button
    private lateinit var taskManager: TaskManager
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskManager = TaskManager.getInstance(this)
        try {
            setContentView(R.layout.activity_main)

            // Verify all views exist
            recyclerView = findViewById(R.id.recyclerView) ?: run {
                throw IllegalStateException("RecyclerView not found in layout")
            }
            tabLayout = findViewById(R.id.tabLayout) ?: run {
                throw IllegalStateException("TabLayout not found in layout")
            }
            fab = findViewById(R.id.fab) ?: run {
                throw IllegalStateException("FAB not found in layout")
            }
            deleteAllBtn = findViewById(R.id.deleteAllBtn) ?: run {
                throw IllegalStateException("DeleteAllBtn not found in layout")
            }

            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = TaskAdapter(
                mutableListOf(),
                onEdit = { task -> showEditDialog(task) },
                onDelete = { task -> deleteTask(task) },
                onComplete = { task -> toggleComplete(task) }
            )
            recyclerView.adapter = adapter

            val itemTouchHelper = ItemTouchHelper(DragCallback(adapter))
            itemTouchHelper.attachToRecyclerView(recyclerView)

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    currentTab = tab?.position ?: 0
                    loadTasks()
                    updateDeleteAllButtonVisibility()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            fab.setOnClickListener {
                showAddDialog()
            }

            deleteAllBtn.setOnClickListener {
                showDeleteAllDialog()
            }

            if (intent?.action == "ADD_TASK") {
                showAddDialog()
            }

            loadTasks()
            updateDeleteAllButtonVisibility()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error to user
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            // Optionally finish the activity or show error screen
        }
    }

    private fun updateDeleteAllButtonVisibility() {
        // Show button only on Completed tab (tab 1) and when there are completed tasks
        val hasCompletedTasks = taskManager.getCompletedTasks().isNotEmpty()
        val shouldShowButton = currentTab == 1 && hasCompletedTasks

        deleteAllBtn.visibility = if (shouldShowButton) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Adjust FAB margin to avoid overlap
        val params = fab.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        val marginBottom = if (shouldShowButton) {
            // Add extra margin when delete button is visible (button height + margins)
            resources.getDimensionPixelSize(android.R.dimen.app_icon_size) + 32
        } else {
            16 * resources.displayMetrics.density.toInt()
        }
        params.bottomMargin = marginBottom
        fab.layoutParams = params
    }

    private fun loadTasks() {
        val tasks = if (currentTab == 0) {
            taskManager.getActiveTasks()
        } else {
            taskManager.getCompletedTasks()
        }
        adapter.updateTasks(tasks)
        updateDeleteAllButtonVisibility()
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = "Task title"

        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    taskManager.addTask(title)
                    loadTasks()
                    TaskWidget.updateWidget(this)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(task: Task) {
        val input = EditText(this)
        input.setText(task.title)

        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    task.title = title
                    taskManager.updateTask(task)
                    loadTasks()
                    TaskWidget.updateWidget(this)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                taskManager.deleteTask(task)
                loadTasks()
                TaskWidget.updateWidget(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllDialog() {
        val completedCount = taskManager.getCompletedTasks().size
        AlertDialog.Builder(this)
            .setTitle("Delete All Completed")
            .setMessage("Are you sure you want to delete all $completedCount completed tasks?")
            .setPositiveButton("Delete All") { _, _ ->
                taskManager.deleteAllCompleted()
                loadTasks()
                TaskWidget.updateWidget(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleComplete(task: Task) {
        task.isCompleted = !task.isCompleted
        taskManager.updateTask(task)
        loadTasks()
        TaskWidget.updateWidget(this)
    }
}

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onComplete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val title: TextView = view.findViewById(R.id.taskTitle)
        val editBtn: ImageButton = view.findViewById(R.id.editBtn)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.checkbox.isChecked = task.isCompleted
        holder.title.text = task.title

        holder.checkbox.setOnClickListener { onComplete(task) }
        holder.editBtn.setOnClickListener { onEdit(task) }
        holder.deleteBtn.setOnClickListener { onDelete(task) }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        Collections.swap(tasks, from, to)
        notifyItemMoved(from, to)
    }

    fun saveOrder(context: android.content.Context) {
        tasks.forEachIndexed { index, task ->
            task.order = index
        }
        TaskManager.getInstance(context).saveTasks(tasks)
    }
}

class DragCallback(private val adapter: TaskAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled() = true

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        adapter.saveOrder(recyclerView.context)
        TaskWidget.updateWidget(recyclerView.context)
    }
}