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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.cubicTo
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.core.domain.model.OrderStatus

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
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    DashboardRefreshArrow(
                        rotation = if (refreshing) loadingRotation else indicatorProgress * 360f,
                        alpha = if (refreshing) 1f else indicatorProgress.coerceAtLeast(0.25f),
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
            .size(23.dp)
            .rotate(rotation),
        tint = Color.White.copy(alpha = alpha),
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
            cubicTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
            cubicTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
            cubicTo(4.01f, 16.42f, 7.58f, 20f, 12f, 20f)
            cubicTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
            lineTo(17.65f, 14f)
            cubicTo(16.82f, 16.33f, 14.61f, 18f, 12f, 18f)
            cubicTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
            cubicTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
            cubicTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
            lineTo(13f, 11f)
            lineTo(20f, 11f)
            lineTo(20f, 4f)
            lineTo(17.65f, 6.35f)
            close()
        }
    }.build()
}
