package com.ssafy.talkeasy.feature.follow.ui.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.ssafy.talkeasy.core.domain.entity.response.Follow
import com.ssafy.talkeasy.feature.common.R as Common
import com.ssafy.talkeasy.feature.common.component.Profile
import com.ssafy.talkeasy.feature.common.ui.theme.cabbage_pont
import com.ssafy.talkeasy.feature.common.ui.theme.delta
import com.ssafy.talkeasy.feature.common.ui.theme.harp
import com.ssafy.talkeasy.feature.common.ui.theme.md_theme_light_background
import com.ssafy.talkeasy.feature.common.ui.theme.sunset_orange
import com.ssafy.talkeasy.feature.common.ui.theme.typography
import com.ssafy.talkeasy.feature.common.util.toTimeString
import com.ssafy.talkeasy.feature.follow.FollowViewModel
import com.ssafy.talkeasy.feature.follow.R

@Composable
internal fun FollowListRoute(
    modifier: Modifier = Modifier,
    navBackStackEntry: NavBackStackEntry,
    viewModel: FollowViewModel = hiltViewModel(navBackStackEntry),
) {
    val followList by rememberUpdatedState(newValue = viewModel.followList.collectAsState().value)
    FollowLisScreen(modifier = modifier, followList = followList ?: arrayListOf())
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
internal fun FollowLisScreen(
    modifier: Modifier = Modifier,
    onClickedAddFriend: () -> Unit = {},
    onClickedNotification: () -> Unit = {},
    onClickedSettings: () -> Unit = {},
    followList: List<Follow> = arrayListOf(),
) {
    Column() {
        FollowListHeader(
            modifier = modifier,
            onClickedAddFriend = onClickedAddFriend,
            onClickedNotification = onClickedNotification,
            onClickedSettings = onClickedSettings
        )

        FollowListContent(modifier = modifier, followList = followList)
    }
}

@Preview(showBackground = true)
@Composable
fun FollowListHeader(
    modifier: Modifier = Modifier,
    onClickedAddFriend: () -> Unit = {},
    onClickedNotification: () -> Unit = {},
    onClickedSettings: () -> Unit = {},
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Text(
                modifier = modifier.padding(18.dp),
                text = stringResource(id = R.string.title_follow_list),
                style = typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(modifier = modifier.padding(end = 18.dp)) {
                IconButton(modifier = modifier, onClick = onClickedAddFriend) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_friend),
                        contentDescription = stringResource(
                            R.string.ic_add_user_text
                        ),
                        modifier = modifier.size(24.dp)
                    )
                }

                IconButton(modifier = modifier, onClick = onClickedNotification) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification_off),
                        contentDescription = stringResource(
                            R.string.ic_notification_text
                        ),
                        modifier = modifier.size(24.dp)
                    )
                }

                IconButton(modifier = modifier, onClick = onClickedSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(
                            R.string.ic_settings_text
                        ),
                        modifier = modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FollowListContent(
    modifier: Modifier = Modifier,
    followList: List<Follow> = arrayListOf(),
) {
    if (followList.isNotEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(items = followList) { index, item ->
                FollowListItem(
                    profileUrl = item.imageUrl,
                    name = item.userName,
                    age = item.age ?: 0,
                    time = "2023-05-04T09:28:32.296943",
                    newMessageCount = 0,
                    gender = if (item.gender == 0) {
                        stringResource(id = R.string.content_man)
                    } else {
                        stringResource(id = R.string.content_woman)
                    }
                )
            }
        }
    } else {
        NoFollowContent(modifier = modifier)
    }
}

@Composable
fun NoFollowContent(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = modifier,
                painter = painterResource(id = Common.drawable.bg_talkeasy_logo_verticcal_trans),
                contentDescription = stringResource(
                    id = R.string.image_logo
                ),
                colorFilter = tint(harp)
            )
            Text(
                text = stringResource(id = R.string.content_no_follow_content),
                style = typography.titleMedium,
                color = harp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FollowListItem(
    modifier: Modifier = Modifier,
    profileUrl: String = "",
    name: String = "",
    gender: String = "여성",
    age: Int = 0,
    time: String = "",
    newMessageCount: Int = 99,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Profile(profileUrl, 56)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = name,
                    style = typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

                Spacer(modifier = modifier.width(10.dp))

                if (time.isNotEmpty()) {
                    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = time.toTimeString(),
                            style = typography.bodySmall,
                            color = delta
                        )
                    }
                }
            }

            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row() {
                    Text(
                        text = String.format(
                            stringResource(
                                id = R.string.content_gender_age,
                                gender,
                                age
                            )
                        ),
                        style = typography.bodyLarge,
                        color = cabbage_pont
                    )
                }

                if (newMessageCount > 0) {
                    Badge(
                        containerColor = sunset_orange,
                        contentColor = md_theme_light_background
                    ) {
                        Text(
                            if (newMessageCount >= 99) {
                                "+99"
                            } else {
                                newMessageCount.toString()
                            }
                        )
                    }
                }
            }
        }
    }
}