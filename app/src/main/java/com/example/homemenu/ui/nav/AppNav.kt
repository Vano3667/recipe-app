package com.example.homemenu.ui.nav

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homemenu.ui.PhotoViewModel
import com.example.homemenu.ui.menu.MenuRoute
import com.example.homemenu.ui.recipes.RecipeDetailRoute
import com.example.homemenu.ui.recipes.RecipeEditRoute
import com.example.homemenu.ui.recipes.RecipesRoute
import com.example.homemenu.ui.settings.SettingsRoute
import com.example.homemenu.ui.shopping.ShoppingRoute

object Routes {
    const val RECIPES = "recipes"
    const val MENU = "menu"
    const val SHOPPING = "shopping"
    const val SETTINGS = "settings"
    const val DETAIL = "recipe/{id}?servings={servings}"
    const val EDIT = "recipe_edit?id={id}"

    fun detail(id: Long, servings: Int? = null) = "recipe/$id" + (servings?.let { "?servings=$it" } ?: "")
    fun edit(id: Long? = null) = if (id == null) "recipe_edit" else "recipe_edit?id=$id"
}

enum class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    RECIPES(Routes.RECIPES, "Рецепты", Icons.Filled.RestaurantMenu),
    MENU(Routes.MENU, "Меню", Icons.Filled.CalendarMonth),
    SHOPPING(Routes.SHOPPING, "Покупки", Icons.Filled.ShoppingCart),
    SETTINGS(Routes.SETTINGS, "Настройки", Icons.Filled.Settings),
}

private fun NavHostController.navigateTop(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppRoot(widthSizeClass: WindowWidthSizeClass) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    var selectedTop by rememberSaveable { mutableStateOf(TopDestination.RECIPES) }
    LaunchedEffect(route) { TopDestination.entries.firstOrNull { it.route == route }?.let { selectedTop = it } }

    val compact = widthSizeClass == WindowWidthSizeClass.Compact
    val expanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val showBars = route != Routes.EDIT

    if (compact) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showBars) NavigationBar(Modifier.testTag("bottom_bar")) {
                    TopDestination.entries.forEach { d ->
                        NavigationBarItem(
                            selected = selectedTop == d,
                            onClick = { nav.navigateTop(d.route) },
                            icon = { Icon(d.icon, null) },
                            label = { Text(d.label) },
                            modifier = Modifier.testTag("nav_${d.name}"),
                        )
                    }
                }
            },
        ) { padding ->
            AppNavHost(nav, compact = true, expanded = false, modifier = Modifier.padding(padding).consumeWindowInsets(padding))
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            if (showBars) NavigationRail(Modifier.testTag("nav_rail"), containerColor = MaterialTheme.colorScheme.surface) {
                TopDestination.entries.forEach { d ->
                    NavigationRailItem(
                        selected = selectedTop == d,
                        onClick = { nav.navigateTop(d.route) },
                        icon = { Icon(d.icon, null) },
                        label = { Text(d.label) },
                        modifier = Modifier.testTag("nav_${d.name}"),
                    )
                }
            }
            val sides = if (showBars) WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom
            else WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical
            AppNavHost(
                nav, compact = false, expanded = expanded,
                modifier = Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(sides)),
            )
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, compact: Boolean, expanded: Boolean, modifier: Modifier) {
    NavHost(nav, startDestination = Routes.RECIPES, modifier = modifier) {
        composable(Routes.RECIPES) {
            val photos: PhotoViewModel = hiltViewModel()
            RecipesRoute(
                expanded = expanded,
                onOpen = { nav.navigate(Routes.detail(it)) },
                onAdd = { nav.navigate(Routes.edit()) },
                onEdit = { nav.navigate(Routes.edit(it)) },
                photo = photos::file,
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("servings") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { entry ->
            val servings = entry.arguments?.getInt("servings")?.takeIf { it > 0 }
            RecipeDetailRoute(
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(it)) },
                onDeleted = { nav.popBackStack() },
                initialServings = servings,
            )
        }
        composable(Routes.EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) {
            RecipeEditRoute(
                onClose = { nav.popBackStack() },
                onSaved = { id ->
                    val cameFromDetail = nav.previousBackStackEntry?.destination?.route == Routes.DETAIL
                    nav.popBackStack()
                    if (compact && !cameFromDetail) nav.navigate(Routes.detail(id)) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.MENU) {
            MenuRoute(
                onCook = { id, servings -> nav.navigate(Routes.detail(id, servings)) },
                onAddRecipe = { nav.navigate(Routes.edit()) },
            )
        }
        composable(Routes.SHOPPING) { ShoppingRoute(onOpenMenu = { nav.navigateTop(Routes.MENU) }) }
        composable(Routes.SETTINGS) { SettingsRoute() }
    }
}
