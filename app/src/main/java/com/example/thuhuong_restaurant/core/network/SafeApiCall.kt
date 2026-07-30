package com.example.thuhuong_restaurant.core.network

import com.example.thuhuong_restaurant.core.common.ApiResult
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

private val gson = Gson()

/**
 * Runs a retrofit suspend call that returns RestResponse<T>, unwraps `data`,
 * and converts 4xx bodies (also RestResponse-shaped) into a readable message.
 */
suspend fun <T> safeApiCall(block: suspend () -> RestResponse<T>): ApiResult<T> {
    return try {
        val response = block()
        val data = response.data
        if (data != null) {
            ApiResult.Success(data)
        } else {
            ApiResult.Failure(response.extractApiError("Không có dữ liệu trả về"))
        }
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string()?.let { body ->
            runCatching { gson.fromJson(body, RestResponse::class.java) }
                .getOrNull()
                ?.extractApiError("Có lỗi xảy ra, vui lòng thử lại")
        }
        ApiResult.Failure(message ?: "Có lỗi xảy ra, vui lòng thử lại")
    } catch (e: IOException) {
        ApiResult.Failure("Không kết nối được máy chủ, vui lòng kiểm tra mạng")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Có lỗi không xác định")
    }
}

/** For endpoints where a 2xx with empty/void body is a valid success (e.g. logout). */
suspend fun safeApiCallUnit(block: suspend () -> Unit): ApiResult<Unit> {
    return try {
        block()
        ApiResult.Success(Unit)
    } catch (e: HttpException) {
        val message = e.response()?.errorBody()?.string()?.let { body ->
            runCatching { gson.fromJson(body, RestResponse::class.java) }
                .getOrNull()
                ?.extractApiError("Có lỗi xảy ra, vui lòng thử lại")
        }
        ApiResult.Failure(message ?: "Có lỗi xảy ra, vui lòng thử lại")
    } catch (e: IOException) {
        ApiResult.Failure("Không kết nối được máy chủ, vui lòng kiểm tra mạng")
    } catch (e: Exception) {
        ApiResult.Failure(e.message ?: "Có lỗi không xác định")
    }
}
