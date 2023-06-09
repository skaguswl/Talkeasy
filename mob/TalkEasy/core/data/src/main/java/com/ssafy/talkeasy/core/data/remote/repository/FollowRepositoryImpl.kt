package com.ssafy.talkeasy.core.data.remote.repository

import com.ssafy.talkeasy.core.data.common.util.wrapToResource
import com.ssafy.talkeasy.core.data.remote.datasource.follow.FollowMemoRequest
import com.ssafy.talkeasy.core.data.remote.datasource.follow.FollowRemoteDataSource
import com.ssafy.talkeasy.core.domain.Resource
import com.ssafy.talkeasy.core.domain.entity.request.SosAlarmRequestBody
import com.ssafy.talkeasy.core.domain.entity.response.Default
import com.ssafy.talkeasy.core.domain.entity.response.Follow
import com.ssafy.talkeasy.core.domain.entity.response.MyNotificationItem
import com.ssafy.talkeasy.core.domain.entity.response.PagingDefault
import com.ssafy.talkeasy.core.domain.repository.FollowRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

class FollowRepositoryImpl @Inject constructor(
    private val followRemoteDataSource: FollowRemoteDataSource,
) : FollowRepository {

    override suspend fun requestFollowList(): Resource<PagingDefault<List<Follow>>> =
        wrapToResource(Dispatchers.IO) {
            val pagingDefaultResponse = followRemoteDataSource.requestFollowList()
            val follow = pagingDefaultResponse.data.map { it.toDomainModel() }
            PagingDefault(
                status = pagingDefaultResponse.status,
                data = follow,
                totalPages = pagingDefaultResponse.totalPages
            )
        }

    override suspend fun requestNotificationList(): Resource<Default<List<MyNotificationItem>>> =
        wrapToResource(Dispatchers.IO) {
            val defaultResponse = followRemoteDataSource.requestNotificationList()
            val notification = defaultResponse.data.map { it.toDomainModel() }
            Default(status = defaultResponse.status, data = notification)
        }

    override suspend fun requestFollow(toUserId: String, memo: String): Resource<String> =
        wrapToResource(Dispatchers.IO) {
            followRemoteDataSource.requestFollow(toUserId, FollowMemoRequest(memo = memo)).data
        }

    override suspend fun modifyFollowMemo(followId: String, memo: String): Resource<Follow> =
        wrapToResource(Dispatchers.IO) {
            followRemoteDataSource.modifyFollowMemo(
                followId,
                FollowMemoRequest(memo = memo)
            ).data.toDomainModel()
        }

    override suspend fun requestSaveWardSOS(
        requestSosAlarmDto: SosAlarmRequestBody,
    ): Resource<Default<String>> =
        wrapToResource(Dispatchers.IO) {
            val defaultResponse = followRemoteDataSource.requestSaveWardSOS(requestSosAlarmDto)
            val str = defaultResponse.data
            Default(status = defaultResponse.status, data = str)
        }
}