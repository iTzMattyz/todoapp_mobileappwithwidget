package com.example.todoappfullkotlin

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("TodoApp", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile
        private var instance: TaskManager? = null

        fun getInstance(context: Context): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getAllTasks(): List<Task> {
        val json = prefs.getString("tasks", "[]")
        val type = object : TypeToken<List<Task>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun getActiveTasks(): List<Task> {
        return getAllTasks().filter { !it.isCompleted }.sortedBy { it.order }
    }

    fun getCompletedTasks(): List<Task> {
        return getAllTasks().filter { it.isCompleted }.sortedBy { it.order }
    }

    fun addTask(title: String) {
        val tasks = getAllTasks().toMutableList()
        val maxOrder = tasks.maxOfOrNull { it.order } ?: -1
        tasks.add(Task(title = title, order = maxOrder + 1))
        saveTasks(tasks)
    }

    fun updateTask(task: Task) {
        val tasks = getAllTasks().toMutableList()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
            saveTasks(tasks)
        }
    }

    fun deleteTask(task: Task) {
        val tasks = getAllTasks().toMutableList()
        tasks.removeAll { it.id == task.id }
        saveTasks(tasks)
    }

    fun saveTasks(tasks: List<Task>) {
        val json = gson.toJson(tasks)
        prefs.edit().putString("tasks", json).commit() // Changed from apply() to commit()
    }

    fun deleteAllCompleted() {
        val tasks = getAllTasks().toMutableList()
        tasks.removeAll { it.isCompleted }
        saveTasks(tasks)
    }
}