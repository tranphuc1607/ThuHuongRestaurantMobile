package com.example.thuhuong_restaurant.feature.employee

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.thuhuong_restaurant.core.navigation.Routes

private data class EmployeeTab(val route: String, val label: String, val icon: ImageVector)

private val EMPLOYEE_TABS = listOf(
    EmployeeTab(Routes.EMPLOYEE_TABLES, "Bàn", Icons.Filled.TableRestaurant),
    EmployeeTab(Routes.EMPLOYEE_ORDERS, "Đơn hàng", Icons.Filled.ReceiptLong),
    EmployeeTab(Routes.EMPLOYEE_BOOKINGS, "Đặt bàn", Icons.Filled.EventNote),
    EmployeeTab(Routes.EMPLOYEE_TODAY_PAYMENTS, "Thu ngân", Icons.Filled.Payments),
    EmployeeTab(Routes.EMPLOYEE_RECEIPT_SCAN, "Quét HĐ", Icons.Filled.CameraAlt),
)

/** Secondary tab row shown on the 5 top-level Employee screens, below the TopAppBar. Scrollable — 5 labels don't fit a fixed row on narrow screens. */
@Composable
fun EmployeeTabRow(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedIndex = EMPLOYEE_TABS.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 12.dp,
    ) {
        EMPLOYEE_TABS.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                text = { Text(tab.label) },
            )
        }
    }
}
