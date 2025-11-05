# Kotlin Todo App & Widget

This is a complete To-Do list application for Android, built entirely in Kotlin. It features a main app for managing tasks and a fully interactive home screen widget for quick access.

The app uses `SharedPreferences` with `Gson` for lightweight and fast data persistence, and a `BroadcastReceiver` to handle widget interactions, making it a great example of connecting app components.

## Features

### 📱 App Features
* **Add, Edit, & Delete Tasks:** Full CRUD functionality through intuitive dialog boxes.
* **Tabbed Interface:** Separate lists for "Active" and "Completed" tasks.
* **Drag-and-Drop Reordering:** Tap and hold active tasks to change their order.
* **Persistent Storage:** Tasks are saved to `SharedPreferences` (as JSON) and are available on app restart.
* **Clear Completed:** A "Delete All" button appears on the "Completed" tab to clear all finished tasks at once.

### 🔳 Widget Features
* **View Active Tasks:** The widget displays a scrollable list of all your "Active" tasks.
* **Complete Tasks from Widget:** Tap the checkbox next to a task on the widget to mark it as complete. The widget and app will sync instantly.
* **Quick Add:** A dedicated "Add" button on the widget opens the app directly to the "Add Task" dialog.
* **Open App:** Tapping the widget's header opens the main application.
* **Real-time Sync:** The widget updates automatically whenever a task is added, completed, or deleted in the app (and vice-versa).

## 🛠️ Technical Breakdown & Architecture

This project is built around a few key components that work together to share data between the main application and the home screen widget.

* **`TaskManager.kt` (Singleton)**
    * This is the **single source of truth** for all task data.
    * It uses the Singleton pattern to ensure the app and the widget are accessing the exact same data instance.
    * It handles all data operations: `getAllTasks`, `addTask`, `updateTask`, etc.
    * Data is serialized to a JSON string using `Gson` and saved synchronously to `SharedPreferences` using `commit()` to guarantee data is written before the widget updates.

* **`MainActivity.kt`**
    * The main screen of the application.
    * Manages the `RecyclerView` (using `TaskAdapter`) and the `TabLayout` for "Active" and "Completed" tabs.
    * Implements the drag-and-drop `ItemTouchHelper` to reorder tasks.
    * Handles all dialogs for adding, editing, and deleting tasks.
    * **Crucially, it calls `TaskWidget.updateWidget(this)` after every task modification** to force the widget to refresh its data.

* **`TaskWidget.kt` (AppWidgetProvider)**
    * The main entry point for the home screen widget.
    * It defines the widget's layout and sets up `PendingIntent`s for interactions.
    * It uses a `RemoteViewsService` (`TaskWidgetService`) to populate the widget's `ListView` with active tasks.
    * It sets up a `PendingIntentTemplate` for the list items, which allows each item to send a unique broadcast.

* **`WidgetActionReceiver.kt` (BroadcastReceiver)**
    * This receiver listens for the `COMPLETE_TASK` action sent by the widget's list items.
    * When an item's checkbox is clicked, this receiver wakes up, gets the `TASK_ID` from the `Intent`, and uses `TaskManager` to update the task's completion status.
    * After updating the task, it calls `TaskWidget.updateWidget()` to refresh the widget (and remove the completed task from its list).

## 📁 Project Files Overview

* **`MainActivity.kt`**: Manages the main app UI, `RecyclerView`, and `TabLayout`. Handles user input for C-R-U-D operations.
* **`TaskManager.kt`**: A singleton class to manage all task data logic, acting as the single source of truth and handling persistence with `SharedPreferences` and `Gson`.
* **`TaskWidget.kt`**: The `AppWidgetProvider` class that defines the widget's behavior, layout, and service intents.
* **`WidgetActionReceiver.kt`**: A `BroadcastReceiver` that listens for and handles "complete task" clicks coming from the widget.
* **`AndroidManifest.xml`**: Declares all necessary components: `Activity`, `Receiver`s (for the widget and its actions), and the `Service` (for the widget's list).

## 🚀 How to Run

1.  Open this project in Android Studio.
2.  Build the project to install dependencies.
3.  Run the app on an Android emulator or a physical device.
4.  Once the app is running, go to your device's home screen, long-press, and select "Widgets" to find and add the "TodoApp" widget.
