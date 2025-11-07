package com.example.todoappfullkotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            when (intent?.action) {
                "COMPLETE_TASK" -> {
                    val taskId = intent.getStringExtra("TASK_ID")
                    taskId?.let {
                        val taskManager = TaskManager.getInstance(ctx)
                        val task = taskManager.getAllTasks().find { it.id == taskId }
                        task?.let { t ->
                            t.isCompleted = !t.isCompleted
                            taskManager.updateTask(t)
                            
                            // Force commit to complete before widget update
                            taskManager.saveTasks(taskManager.getAllTasks())
                            
                            // Longer delay to ensure cross-process sync
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                TaskWidget.updateWidget(ctx)
                            }, 250)
                        }
                    }
                }
            }
        }
    }
}