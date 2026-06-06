package com.iptv.player.core.network

/** Wraps the outcome of a network/repository call so the UI can react without try/catch everywhere. */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
    is NetworkResult.Exception -> this
}

/** Runs [block], translating Retrofit/OkHttp throwables into a [NetworkResult]. */
suspend fun <T> safeApiCall(block: suspend () -> retrofit2.Response<T>): NetworkResult<T> = try {
    val response = block()
    val body = response.body()
    if (response.isSuccessful && body != null) {
        NetworkResult.Success(body)
    } else {
        NetworkResult.Error(response.code(), response.message().ifBlank { "HTTP ${response.code()}" })
    }
} catch (t: Throwable) {
    NetworkResult.Exception(t)
}
