package com.trufflear.search.frameworks

/**
 * [Result] is a generic `Either` class that holds either a `Success` value or an `Error` value.
 * Recommended for holding data from network responses.
 */
sealed class Result<out A : Any, out B : Any> {

    data class Success<A : Any>(val value: A) : Result<A, Nothing>()
    data class Error<B : Any>(val error: B) : Result<Nothing, B>()
}

/**
 * Returns success value if it exists, otherwise returns null.
 */
fun <T : Any, E : Any> Result<T, E>.successValueOrNull(): T? =
    (this as? Result.Success)?.value
