package com.amenitypay.sdk.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Result")
class ResultTest {

    @Nested
    @DisplayName("Success")
    inner class SuccessTest {

        @Test
        @DisplayName("isSuccess returns true")
        fun `isSuccess returns true`() {
            val result: Result<String> = Result.Success("data")
            
            assertThat(result.isSuccess).isTrue()
            assertThat(result.isError).isFalse()
            assertThat(result.isLoading).isFalse()
        }

        @Test
        @DisplayName("getOrNull returns data")
        fun `getOrNull returns data`() {
            val result: Result<String> = Result.Success("test data")
            
            assertThat(result.getOrNull()).isEqualTo("test data")
        }

        @Test
        @DisplayName("getOrThrow returns data")
        fun `getOrThrow returns data`() {
            val result: Result<Int> = Result.Success(42)
            
            assertThat(result.getOrThrow()).isEqualTo(42)
        }

        @Test
        @DisplayName("map transforms data")
        fun `map transforms data`() {
            val result: Result<Int> = Result.Success(10)
            val mapped = result.map { it * 2 }
            
            assertThat(mapped).isInstanceOf(Result.Success::class.java)
            assertThat((mapped as Result.Success).data).isEqualTo(20)
        }

        @Test
        @DisplayName("onSuccess executes action")
        fun `onSuccess executes action`() {
            var captured: String? = null
            val result: Result<String> = Result.Success("hello")
            
            result.onSuccess { captured = it }
            
            assertThat(captured).isEqualTo("hello")
        }

        @Test
        @DisplayName("onError does not execute action")
        fun `onError does not execute action`() {
            var executed = false
            val result: Result<String> = Result.Success("hello")
            
            result.onError { _, _ -> executed = true }
            
            assertThat(executed).isFalse()
        }
    }

    @Nested
    @DisplayName("Error")
    inner class ErrorTest {

        @Test
        @DisplayName("isError returns true")
        fun `isError returns true`() {
            val result: Result<String> = Result.Error("CODE", "message")
            
            assertThat(result.isError).isTrue()
            assertThat(result.isSuccess).isFalse()
            assertThat(result.isLoading).isFalse()
        }

        @Test
        @DisplayName("getOrNull returns null")
        fun `getOrNull returns null`() {
            val result: Result<String> = Result.Error("CODE", "message")
            
            assertThat(result.getOrNull()).isNull()
        }

        @Test
        @DisplayName("getOrThrow throws exception")
        fun `getOrThrow throws exception`() {
            val result: Result<String> = Result.Error("CODE", "error message")
            
            val exception = assertThrows<Exception> {
                result.getOrThrow()
            }
            
            assertThat(exception.message).isEqualTo("error message")
        }

        @Test
        @DisplayName("map returns same error")
        fun `map returns same error`() {
            val result: Result<Int> = Result.Error("CODE", "message")
            val mapped = result.map { it * 2 }
            
            assertThat(mapped).isInstanceOf(Result.Error::class.java)
            assertThat((mapped as Result.Error).code).isEqualTo("CODE")
        }

        @Test
        @DisplayName("onError executes action")
        fun `onError executes action`() {
            var capturedCode: String? = null
            var capturedMessage: String? = null
            val result: Result<String> = Result.Error("ERR_CODE", "Error occurred")
            
            result.onError { code, message -> 
                capturedCode = code
                capturedMessage = message
            }
            
            assertThat(capturedCode).isEqualTo("ERR_CODE")
            assertThat(capturedMessage).isEqualTo("Error occurred")
        }

        @Test
        @DisplayName("onSuccess does not execute action")
        fun `onSuccess does not execute action`() {
            var executed = false
            val result: Result<String> = Result.Error("CODE", "message")
            
            result.onSuccess { executed = true }
            
            assertThat(executed).isFalse()
        }
    }

    @Nested
    @DisplayName("Loading")
    inner class LoadingTest {

        @Test
        @DisplayName("isLoading returns true")
        fun `isLoading returns true`() {
            val result: Result<String> = Result.Loading
            
            assertThat(result.isLoading).isTrue()
            assertThat(result.isSuccess).isFalse()
            assertThat(result.isError).isFalse()
        }

        @Test
        @DisplayName("getOrNull returns null")
        fun `getOrNull returns null`() {
            val result: Result<String> = Result.Loading
            
            assertThat(result.getOrNull()).isNull()
        }

        @Test
        @DisplayName("getOrThrow throws IllegalStateException")
        fun `getOrThrow throws IllegalStateException`() {
            val result: Result<String> = Result.Loading
            
            assertThrows<IllegalStateException> {
                result.getOrThrow()
            }
        }

        @Test
        @DisplayName("map returns Loading")
        fun `map returns Loading`() {
            val result: Result<Int> = Result.Loading
            val mapped = result.map { it * 2 }
            
            assertThat(mapped).isEqualTo(Result.Loading)
        }
    }
}
