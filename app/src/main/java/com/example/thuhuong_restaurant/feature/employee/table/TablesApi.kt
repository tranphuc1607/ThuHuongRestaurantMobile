package com.example.thuhuong_restaurant.feature.employee.table

import com.example.thuhuong_restaurant.core.model.TableResponse
import com.example.thuhuong_restaurant.core.network.RestResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface TablesApi {
    @GET("orders/tables")
    suspend fun getTables(): RestResponse<List<TableResponse>>

    @GET("orders/tables/{id}")
    suspend fun getTable(@Path("id") id: String): RestResponse<TableResponse>
}
