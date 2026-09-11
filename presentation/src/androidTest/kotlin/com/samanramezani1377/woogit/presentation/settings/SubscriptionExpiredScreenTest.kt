package com.samanramezani1377.woogit.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SubscriptionExpiredScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var previousGateway = BillingRuntime.gateway

    @Before
    fun forceBillingUnavailableState() {
        previousGateway = BillingRuntime.gateway
        BillingRuntime.gateway = null
    }

    @After
    fun restoreBillingGateway() {
        BillingRuntime.gateway = previousGateway
    }

    @Test
    fun expiredScreenRendersLockedSubscriptionUi() {
        composeRule.setContent {
            SubscriptionExpiredScreen(
                storeId = StoreId("ui-test-store"),
                onSubscriptionRestored = {},
            )
        }

        composeRule.onNodeWithText("اشتراک منقضی شده است").assertIsDisplayed()
        composeRule.onNodeWithText("سیستم پرداخت در دسترس نیست. لطفاً بعداً دوباره تلاش کنید.").assertIsDisplayed()
    }
}
