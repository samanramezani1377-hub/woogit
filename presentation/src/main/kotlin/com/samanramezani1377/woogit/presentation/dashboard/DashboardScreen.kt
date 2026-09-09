package com.samanramezani1377.woogit.presentation.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus

private val DashboardRefreshAccent = Color(0xFF6C5CE7)

@Composable
internal fun DashboardScreen(
    storeName: String,
    connected: Boolean,
    orders: String,
    products: String,
    revenue: String,
    pending: String,
    recentOrderId: String?,
    recentCustomer: String,
    recentTotal: String,
    recentStatus: OrderStatus?,
    onRecentOrderClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
    onConflictsClick: () -> Unit,
    selectedDestination: DashboardDestination = DashboardDestination.DASHBOARD,
    onDestinationSelected: (DashboardDestination) -> Unit,
    onAiClick: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val pullThreshold = 400f
    val pullToReloadConnection = remember(scrollState, refreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (refreshing) return Offset.Zero
                if (scrollState.value == 0 && available.y > 0f) {
                    pullDistance = (pullDistance + available.y).coerceAtMost(pullThreshold)
                    return Offset.Zero
                }
                if (scrollState.value == 0 && available.y < 0f && pullDistance > 0f) {
                    pullDistance = (pullDistance + available.y).coerceAtLeast(0f)
                    return Offset.Zero
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!refreshing && pullDistance >= pullThreshold) {
                    pullDistance = 0f
                    onRefresh()
                } else {
                    pullDistance = 0f
                }
                return Velocity.Zero
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "dashboardRefresh")
    val loadingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashboardRefreshRotation",
    )

    Column(
        modifier
            .fillMaxSize()
            .nestedScroll(pullToReloadConnection),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 8.dp),
            ) {
                DashboardContent(storeName, connected, orders, products, revenue, pending, recentOrderId, recentCustomer, recentTotal, recentStatus, onRecentOrderClick)
                DashboardActions(onOrdersClick, onProductsClick, onSettingsClick, onSyncClick, onConflictsClick, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            val indicatorProgress = (pullDistance / pullThreshold).coerceIn(0f, 1f)
            if (pullDistance > 0f || refreshing) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center,
                ) {
                    DashboardRefreshArrow(
                        rotation = if (refreshing) loadingRotation else -indicatorProgress * 360f,
                        alpha = if (refreshing) 0.92f else indicatorProgress.coerceAtLeast(0.25f),
                    )
                }
            }
        }
        DashboardFloatingNavigation(
            selected = selectedDestination,
            onDestinationSelected = onDestinationSelected,
            onAiClick = onAiClick,
        )
    }
}

@Composable
private fun DashboardRefreshArrow(rotation: Float, alpha: Float) {
    androidx.compose.material3.Icon(
        imageVector = DashboardRefreshIcon,
        contentDescription = "در حال تازه‌سازی",
        modifier = Modifier
            .size(36.dp)
            .scale(scaleX = -1f, scaleY = 1f)
            .rotate(rotation),
        tint = DashboardRefreshAccent.copy(alpha = alpha),
    )
}

private val DashboardRefreshIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DashboardRefresh",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(17.65f, 6.35f)
            lineTo(15.95f, 8.05f)
            lineTo(14.95f, 7.05f)
            lineTo(13.85f, 6.55f)
            lineTo(12.65f, 6.15f)
            lineTo(11.4f, 6.05f)
            lineTo(10.15f, 6.2f)
            lineTo(8.95f, 6.65f)
            lineTo(7.9f, 7.35f)
            lineTo(6.95f, 8.3f)
            lineTo(6.25f, 9.4f)
            lineTo(5.8f, 10.6f)
            lineTo(5.65f, 12f)
            lineTo(5.8f, 13.4f)
            lineTo(6.25f, 14.6f)
            lineTo(6.95f, 15.7f)
            lineTo(7.9f, 16.65f)
            lineTo(8.95f, 17.35f)
            lineTo(10.15f, 17.8f)
            lineTo(11.4f, 17.95f)
            lineTo(12.65f, 17.85f)
            lineTo(13.85f, 17.45f)
            lineTo(14.95f, 16.95f)
            lineTo(15.9f, 16.05f)
            lineTo(17.65f, 14.3f)
            lineTo(17.65f, 16.75f)
            lineTo(19.65f, 14.75f)
            lineTo(19.65f, 11.25f)
            lineTo(16.15f, 11.25f)
            lineTo(14.15f, 13.25f)
            lineTo(16.15f, 13.25f)
            lineTo(15.05f, 14.35f)
            lineTo(14.3f, 14.95f)
            lineTo(13.4f, 15.4f)
            lineTo(12.4f, 15.65f)
            lineTo(11.4f, 15.7f)
            lineTo(10.4f, 15.55f)
            lineTo(9.45f, 15.2f)
            lineTo(8.6f, 14.6f)
            lineTo(7.9f, 13.8f)
            lineTo(7.4f, 12.9f)
            lineTo(7.15f, 11.95f)
            lineTo(7.25f, 10.95f)
            lineTo(7.65f, 10.05f)
            lineTo(8.3f, 9.25f)
            lineTo(9.15f, 8.6f)
            lineTo(10.1f, 8.2f)
            lineTo(11.1f, 8.05f)
            lineTo(12.1f, 8.15f)
            lineTo(13f, 8.5f)
            lineTo(13.8f, 9.1f)
            lineTo(14.5f, 9.85f)
            lineTo(16.2f, 8.15f)
            lineTo(17.65f, 6.35f)
            close()
        }
    }.build()
}
