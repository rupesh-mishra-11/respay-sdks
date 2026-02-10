package com.amenitypay.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T?,
    @SerializedName("error")
    val error: ApiError?
)

/**
 * API Error model
 */
data class ApiError(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("details")
    val details: Map<String, String>? = null
)

/**
 * Sealed class for handling API results
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: String, val message: String, val exception: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
    
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    
    fun getOrNull(): T? = (this as? Success)?.data
    
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw exception ?: Exception(message)
            is Loading -> throw IllegalStateException("Result is still loading")
        }
    }
    
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> Loading
        }
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (String, String) -> Unit): Result<T> {
        if (this is Error) action(code, message)
        return this
    }
}
