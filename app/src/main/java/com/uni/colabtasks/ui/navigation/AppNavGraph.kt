package com.uni.colabtasks.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uni.colabtasks.ui.auth.AuthGateViewModel
import com.uni.colabtasks.ui.auth.login.LoginScreen
import com.uni.colabtasks.ui.auth.signup.SignUpScreen
import com.uni.colabtasks.ui.calendar.CalendarScreen
import com.uni.colabtasks.ui.common.components.AppDrawerContent
import com.uni.colabtasks.ui.common.components.LoadingIndicator
import com.uni.colabtasks.ui.settings.SettingsScreen
import com.uni.colabtasks.ui.taskedit.TaskEditScreen
import com.uni.colabtasks.ui.tasklists.TaskListsScreen
import com.uni.colabtasks.ui.tasks.TasksScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(authGateViewModel: AuthGateViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isAuthenticated by authGateViewModel.isAuthenticated.collectAsState()

    if (isAuthenticated == null) {
        LoadingIndicator()
        return
    }

    val authenticated = isAuthenticated == true
    val startDestination = if (authenticated) Destinations.TASK_LISTS else Destinations.LOGIN

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentSection = DrawerSection.fromRoute(backStackEntry?.destination?.route)
    val drawerEnabled = currentSection != null && authenticated

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerEnabled,
        drawerContent = {
            if (drawerEnabled) {
                AppDrawerContent(
                    current = currentSection ?: DrawerSection.LISTS,
                    onSelect = { section ->
                        scope.launch { drawerState.close() }
                        navigateToDrawerSection(navController, section)
                    }
                )
            }
        }
    ) {
        AppGraphContent(
            navController = navController,
            startDestination = startDestination,
            onOpenDrawer = { scope.launch { drawerState.open() } }
        )
    }
}

@Composable
private fun AppGraphContent(
    navController: NavHostController,
    startDestination: String,
    onOpenDrawer: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Destinations.LOGIN) {
            LoginScreen(
                onSignedIn = {
                    navController.navigate(Destinations.TASK_LISTS) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                },
                onGoToSignUp = { navController.navigate(Destinations.SIGNUP) }
            )
        }

        composable(Destinations.SIGNUP) {
            SignUpScreen(
                onSignedUp = {
                    navController.navigate(Destinations.TASK_LISTS) {
                        popUpTo(Destinations.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.TASK_LISTS) {
            TaskListsScreen(
                onMenuClick = onOpenDrawer,
                onProfileClick = { navController.navigate(Destinations.SETTINGS) },
                onOpenList = { listId -> navController.navigate(Destinations.tasks(listId)) }
            )
        }

        composable(Destinations.CALENDAR) {
            CalendarScreen(
                onMenuClick = onOpenDrawer,
                onProfileClick = { navController.navigate(Destinations.SETTINGS) },
                onOpenTask = { task ->
                    navController.navigate(Destinations.taskEdit(task.listId, task.id))
                }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onMenuClick = onOpenDrawer,
                onSignedOut = {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Destinations.TASKS_ROUTE,
            arguments = listOf(navArgument(Destinations.ARG_LIST_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString(Destinations.ARG_LIST_ID).orEmpty()
            TasksScreen(
                onBack = { navController.popBackStack() },
                onAddTask = { navController.navigate(Destinations.taskEdit(listId)) },
                onEditTask = { taskId -> navController.navigate(Destinations.taskEdit(listId, taskId)) }
            )
        }

        composable(
            route = Destinations.TASK_EDIT_ROUTE,
            arguments = listOf(
                navArgument(Destinations.ARG_LIST_ID) { type = NavType.StringType },
                navArgument(Destinations.ARG_TASK_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TaskEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}

private fun navigateToDrawerSection(navController: NavHostController, section: DrawerSection) {
    navController.navigate(section.route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
