package com.example.todoappfullkotlin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class TaskWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TaskWidget::class.java)
            )

            // Update each widget
            for (appWidgetId in ids) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Force data refresh first
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetTaskList)

            // Get fresh task count and update counter
            val taskManager = TaskManager.getInstance(context)
            val pendingCount = taskManager.getActiveTasks().size

            views.setTextViewText(
                R.id.widgetPendingCount,
                if (pendingCount == 1) "1 pending" else "$pendingCount pending"
            )

            // Set up the intent for the ListView
            val serviceIntent = Intent(context, TaskWidgetService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            views.setRemoteAdapter(R.id.widgetTaskList, serviceIntent)

            // Set up the empty view
            views.setEmptyView(R.id.widgetTaskList, R.id.widgetEmptyView)

            // Set up item click template for checkbox
            val checkIntent = Intent(context, WidgetActionReceiver::class.java)
            checkIntent.action = "COMPLETE_TASK"
            val checkPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                checkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widgetTaskList, checkPendingIntent)

            // Add task button
            val addIntent = Intent(context, MainActivity::class.java)
            addIntent.action = "ADD_TASK"
            addIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val addPendingIntent = PendingIntent.getActivity(
                context,
                0,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetAddButton, addPendingIntent)

            // Open app intent
            val openIntent = Intent(context, MainActivity::class.java)
            val openPendingIntent = PendingIntent.getActivity(
                context,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetHeader, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            context?.let {
                val appWidgetManager = AppWidgetManager.getInstance(it)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ids?.let { idArray -> onUpdate(it, appWidgetManager, idArray) }
            }
        }
    }
}

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskRemoteViewsFactory(this.applicationContext)
    }
}

class TaskRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var tasks = listOf<Task>()

    override fun onCreate() {
        loadTasks()
    }

    override fun onDataSetChanged() {
        loadTasks()
    }

    private fun loadTasks() {
        tasks = TaskManager.getInstance(context).getActiveTasks()
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task_item)

        if (position < tasks.size) {
            val task = tasks[position]
            views.setTextViewText(R.id.widgetTaskTitle, task.title)
            views.setImageViewResource(
                R.id.widgetCheckbox,
                if (task.isCompleted) R.drawable.ic_checked else R.drawable.ic_unchecked
            )

            // Set up fill-in intent for this item
            val fillInIntent = Intent()
            fillInIntent.putExtra("TASK_ID", task.id)
            views.setOnClickFillInIntent(R.id.widgetCheckbox, fillInIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}