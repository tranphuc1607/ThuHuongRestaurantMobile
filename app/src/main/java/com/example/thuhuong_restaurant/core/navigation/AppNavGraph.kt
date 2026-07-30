package com.example.thuhuong_restaurant.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.thuhuong_restaurant.feature.account.AccountScreen
import com.example.thuhuong_restaurant.feature.auth.AuthViewModel
import com.example.thuhuong_restaurant.feature.auth.LoginScreen
import com.example.thuhuong_restaurant.feature.auth.RegisterScreen
import com.example.thuhuong_restaurant.feature.employee.booking.BookingDetailScreen
import com.example.thuhuong_restaurant.feature.employee.booking.BookingListScreen
import com.example.thuhuong_restaurant.feature.employee.order.OrderDetailScreen
import com.example.thuhuong_restaurant.feature.employee.orderlist.OrderListScreen
import com.example.thuhuong_restaurant.feature.employee.payment.PaymentScreen
import com.example.thuhuong_restaurant.feature.employee.payment.TodayPaymentsScreen
import com.example.thuhuong_restaurant.feature.employee.receiptscan.ReceiptScanScreen
import com.example.thuhuong_restaurant.feature.employee.table.TableMapScreen
import com.example.thuhuong_restaurant.feature.menu.MenuScreen
import com.example.thuhuong_restaurant.feature.menu.ProductDetailScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
) {
    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MenuScreen(onProductClick = { id -> navController.navigate(Routes.productDetail(id)) })
        }
        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
        ) {
            ProductDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.EMPLOYEE_TABLES) {
            TableMapScreen(
                navController = navController,
                onOrderReady = { orderId -> navController.navigate(Routes.employeeOrderDetail(orderId)) },
            )
        }
        composable(Routes.EMPLOYEE_ORDERS) {
            OrderListScreen(
                navController = navController,
                onManageOrder = { orderId -> navController.navigate(Routes.employeeOrderDetail(orderId)) },
                onPayOrder = { orderId -> navController.navigate(Routes.employeePayment(orderId)) },
            )
        }
        composable(Routes.EMPLOYEE_BOOKINGS) {
            BookingListScreen(
                navController = navController,
                onOpenDetail = { code -> navController.navigate(Routes.employeeBookingDetail(code)) },
                onOrderReady = { orderId -> navController.navigate(Routes.employeeOrderDetail(orderId)) },
            )
        }
        composable(Routes.EMPLOYEE_TODAY_PAYMENTS) {
            TodayPaymentsScreen(navController = navController)
        }
        composable(Routes.EMPLOYEE_RECEIPT_SCAN) {
            ReceiptScanScreen(
                navController = navController,
                onOrderReady = { orderId -> navController.navigate(Routes.employeeOrderDetail(orderId)) },
            )
        }
        composable(
            route = Routes.EMPLOYEE_BOOKING_DETAIL,
            arguments = listOf(navArgument("bookingCode") { type = NavType.StringType }),
        ) {
            BookingDetailScreen(
                onBack = { navController.popBackStack() },
                onOrderReady = { orderId -> navController.navigate(Routes.employeeOrderDetail(orderId)) },
            )
        }
        composable(
            route = Routes.EMPLOYEE_ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) {
            OrderDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPayment = { orderId -> navController.navigate(Routes.employeePayment(orderId)) },
            )
        }
        composable(
            route = Routes.EMPLOYEE_PAYMENT,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) {
            PaymentScreen(
                onBack = { navController.popBackStack() },
                onPaymentCompleted = {
                    navController.popBackStack(Routes.EMPLOYEE_TABLES, inclusive = false)
                },
            )
        }
    }
}
