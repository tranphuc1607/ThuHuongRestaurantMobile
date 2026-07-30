package com.example.thuhuong_restaurant.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.thuhuong_restaurant.feature.auth.AuthViewModel

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val baseNavItems = listOf(
    BottomNavItem(Routes.MENU, "Thực đơn", Icons.Filled.RestaurantMenu),
    BottomNavItem(Routes.ACCOUNT, "Tài khoản", Icons.Filled.Person),
)

private val employeeNavItem = BottomNavItem(Routes.EMPLOYEE_TABLES, "Nhân viên", Icons.Filled.TableRestaurant)

@Composable
fun BottomNavBar(navController: NavHostController, authViewModel: AuthViewModel) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    if (currentRoute !in Routes.bottomNavRoutes) return

    val user by authViewModel.currentUser.collectAsState()
    val isStaff = user?.role == "ROLE_EMPLOYEE" || user?.role == "ROLE_ADMIN"
    val bottomNavItems = if (isStaff) baseNavItems + employeeNavItem else baseNavItems

    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
