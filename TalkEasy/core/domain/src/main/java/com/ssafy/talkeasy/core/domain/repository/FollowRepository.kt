package com.ssafy.talkeasy.core.domain.repository

import com.ssafy.talkeasy.core.domain.Resource
import com.ssafy.talkeasy.core.domain.entity.response.Follow
import com.ssafy.talkeasy.core.domain.entity.response.PagingDefault

interface FollowRepository {

    suspend fun requestFollowList(): Resource<PagingDefault<List<Follow>>>
}