package com.uni.colabtasks.ui.navigation

object Destinations {
    const val LOGIN = "login"
    const val SIGNUP = "signup"

    // Drawer-level destinations
    const val TASK_LISTS = "task_lists"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"

    // tasks/{listId}
    const val TASKS_ROUTE = "tasks/{listId}"
    const val ARG_LIST_ID = "listId"
    fun tasks(listId: String) = "tasks/$listId"

    // task_edit/{listId}?taskId={taskId}
    const val TASK_EDIT_ROUTE = "task_edit/{listId}?taskId={taskId}"
    const val ARG_TASK_ID = "taskId"
    fun taskEdit(listId: String, taskId: String? = null): String =
        if (taskId == null) "task_edit/$listId" else "task_edit/$listId?taskId=$taskId"

    // activity/{listId}
    const val ACTIVITY_ROUTE = "activity/{listId}"
    fun activity(listId: String) = "activity/$listId"
}

enum class DrawerSection(val route: String) {
    LISTS(Destinations.TASK_LISTS),
    CALENDAR(Destinations.CALENDAR),
    SETTINGS(Destinations.SETTINGS);

    companion object {
        fun fromRoute(route: String?): DrawerSection? = entries.firstOrNull { it.route == route }
    }
}
