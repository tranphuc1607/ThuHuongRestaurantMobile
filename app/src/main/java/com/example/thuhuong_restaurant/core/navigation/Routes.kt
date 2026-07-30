package com.example.thuhuong_restaurant.core.navigation

object Routes {
    const val MENU = "menu"
    const val ACCOUNT = "account"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PRODUCT_DETAIL = "product/{productId}"

    const val EMPLOYEE_TABLES = "employee/tables"
    const val EMPLOYEE_ORDERS = "employee/orders"
    const val EMPLOYEE_BOOKINGS = "employee/bookings"
    const val EMPLOYEE_TODAY_PAYMENTS = "employee/today-payments"
    const val EMPLOYEE_RECEIPT_SCAN = "employee/receipt-scan"
    const val EMPLOYEE_ORDER_DETAIL = "employee/orders/{orderId}"
    const val EMPLOYEE_PAYMENT = "employee/payment/{orderId}"
    const val EMPLOYEE_BOOKING_DETAIL = "employee/bookings/{bookingCode}"

    fun productDetail(id: String) = "product/$id"
    fun employeeOrderDetail(orderId: String) = "employee/orders/$orderId"
    fun employeePayment(orderId: String) = "employee/payment/$orderId"
    fun employeeBookingDetail(bookingCode: String) = "employee/bookings/$bookingCode"

    val bottomNavRoutes = setOf(
        MENU, ACCOUNT, EMPLOYEE_TABLES, EMPLOYEE_ORDERS, EMPLOYEE_BOOKINGS, EMPLOYEE_TODAY_PAYMENTS, EMPLOYEE_RECEIPT_SCAN,
    )

    /** Routes that render the 5-tab EmployeeTabRow (Bàn / Đơn hàng / Đặt bàn / Thu ngân / Quét HĐ). */
    val employeeTabRoutes = listOf(
        EMPLOYEE_TABLES, EMPLOYEE_ORDERS, EMPLOYEE_BOOKINGS, EMPLOYEE_TODAY_PAYMENTS, EMPLOYEE_RECEIPT_SCAN,
    )
}
