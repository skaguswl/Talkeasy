package com.ssafy.talkeasy.core.data.common.util

import com.ssafy.talkeasy.core.domain.Resource
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import retrofit2.HttpException

suspend fun <T> wrapToResource(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> T,
): Resource<T> {
    return try {
        Resource.Success(apiCall())
    } catch (throwable: Throwable) {
        when (throwable) {
            is IOException -> Resource.Error(throwable.message ?: "ERROR1")
            is HttpException -> {
                val code = throwable.code()
                Resource.Error(code.toString())
            }
            else -> {
                Resource.Error(throwable.message ?: "ERROR2")
            }
        }
    }
}