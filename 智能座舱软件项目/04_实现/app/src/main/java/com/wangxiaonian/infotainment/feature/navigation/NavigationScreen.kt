package com.wangxiaonian.infotainment.feature.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wangxiaonian.infotainment.presentation.component.CarCard
import com.wangxiaonian.infotainment.presentation.component.CarListItem

/**
 * 导航页面
 *
 * @author 王小年联盟
 * @version 1.0
 */
@Composable
fun NavigationScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.navigationState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            NavigationTopBar(onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 搜索框
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.searchDestination(it) },
                onSearch = { viewModel.searchDestination(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isNavigating && state.currentDestination != null) {
                // 导航中状态
                NavigationActiveCard(
                    destination = state.currentDestination!!,
                    routeInfo = state.routeInfo,
                    onStopNavigation = { viewModel.stopNavigation() }
                )
            } else if (searchResults.isNotEmpty()) {
                // 搜索结果
                Text(
                    text = "搜索结果",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults) { destination ->
                        DestinationCard(
                            destination = destination,
                            onClick = { viewModel.selectDestination(destination) },
                            onStartNavigation = { viewModel.startNavigation(destination) }
                        )
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                // 无结果
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到相关地点",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 默认状态 - 快速入口
                QuickAccessSection(
                    onHomeClick = { /* TODO */ },
                    onWorkClick = { /* TODO */ },
                    onFavoritesClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun NavigationTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "导航",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    CarCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜索目的地") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                singleLine = true
            )
        }
    }
}

@Composable
private fun DestinationCard(
    destination: NavigationDestination,
    onClick: () -> Unit,
    onStartNavigation: () -> Unit
) {
    CarCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = destination.address,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onStartNavigation,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text("导航")
            }
        }
    }
}

@Composable
private fun NavigationActiveCard(
    destination: NavigationDestination,
    routeInfo: RouteInfo?,
    onStopNavigation: () -> Unit
) {
    CarCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "导航中",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = destination.name,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = destination.address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (routeInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RouteInfoItem(label = "距离", value = routeInfo.distance)
                    RouteInfoItem(label = "预计时间", value = routeInfo.duration)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStopNavigation,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("结束导航")
            }
        }
    }
}

@Composable
private fun RouteInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun QuickAccessSection(
    onHomeClick: () -> Unit,
    onWorkClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    Text(
        text = "快速导航",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickAccessCard(
            title = "回家",
            onClick = onHomeClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessCard(
            title = "去公司",
            onClick = onWorkClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessCard(
            title = "收藏",
            onClick = onFavoritesClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CarCard(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.5f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
