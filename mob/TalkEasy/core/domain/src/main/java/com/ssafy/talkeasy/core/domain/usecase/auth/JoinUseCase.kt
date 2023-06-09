package com.ssafy.talkeasy.core.domain.usecase.auth

import com.ssafy.talkeasy.core.domain.Resource
import com.ssafy.talkeasy.core.domain.entity.request.MemberRequestBody
import com.ssafy.talkeasy.core.domain.entity.response.Default
import com.ssafy.talkeasy.core.domain.repository.AuthRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JoinUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {

    suspend operator fun invoke(
        member: MemberRequestBody,
        image: File?,
    ): Resource<Default<String>> =
        withContext(Dispatchers.IO) {
            authRepository.requestJoin(member, image)
        }
}