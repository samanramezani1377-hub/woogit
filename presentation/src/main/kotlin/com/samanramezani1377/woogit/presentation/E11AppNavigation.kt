package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.connection.ConnectionScreen
import com.samanramezani1377.woogit.presentation.dashboard.DashboardDestination
import com.samanramezani1377.woogit.presentation.dashboard.DashboardScreen
import com.samanramezani1377.woogit.presentation.dashboard.DashboardViewModel
import com.samanramezani1377.woogit.presentation.dashboard.DashboardViewModelFactory
import com.samanramezani1377.woogit.presentation.order.OrderDetailScreen
import com.samanramezani1377.woogit.presentation.order.OrderDetailUiState
import com.samanramezani1377.woogit.presentation.orders.OrderRowUiModel
import com.samanramezani1377.woogit.presentation.orders.OrdersScreen
import com.samanramezani1377.woogit.presentation.orders.OrdersUiState
import com.samanramezani1377.woogit.presentation.product.ProductDetailScreen
import com.samanramezani1377.woogit.presentation.product.ProductEditorRoute
import com.samanramezani1377.woogit.presentation.product.ProductsScreen
import com.samanramezani1377.woogit.presentation.product.VariationEditorRoute
import com.samanramezani1377.woogit.presentation.product.VariationUiModel
import com.samanramezani1377.woogit.presentation.product.VariationsManagementScreen
import com.samanramezani1377.woogit.presentation.sync.ConflictsScreen
import com.samanramezani1377.woogit.presentation.sync.ConflictUiModel
import com.samanramezani1377.woogit.presentation.sync.SyncScreen
import com.samanramezani1377.woogit.presentation.sync.SyncUiState

@Composable
internal fun E11AppNavigation(dependencies:V1PresentationDependencies,initialOrderId:String?){
    val navController=rememberNavController();var activeStore by rememberSaveable{mutableStateOf(dependencies.initialStoreId)}
    val startDestination=when{activeStore==null->E11Routes.CONNECTION;initialOrderId!=null->E11Routes.order(initialOrderId);else->E11Routes.DASHBOARD}
    NavHost(navController,startDestination){
        composable(E11Routes.CONNECTION){ConnectionScreen(dependencies){storeId->activeStore=storeId;navController.navigate(E11Routes.DASHBOARD){popUpTo(E11Routes.CONNECTION){inclusive=true}}}}
        composable(E11Routes.DASHBOARD){val s=activeStore?.let(::StoreId);if(s==null){LaunchedEffect(Unit){navController.navigate(E11Routes.CONNECTION)}}else{val vm=viewModel<DashboardViewModel>(key="dashboard-${s.value}",factory=DashboardViewModelFactory(dependencies,s));val st by vm.uiState.collectAsState();LaunchedEffect(s){vm.refresh()};val r=st.orders.firstOrNull();DashboardScreen(s.value,true,st.ordersCount,st.productsCount,st.revenue,st.pendingCount,r?.number,r?.customer?.name.orEmpty(),formatMoney(r?.total),r?.status,{r?.number?.let{navController.navigate(E11Routes.order(it))}},{navController.navigate(E11Routes.ORDERS)},{navController.navigate(E11Routes.PRODUCTS)},{navController.navigate(E11Routes.SETTINGS)},{navController.navigate(E11Routes.SYNC)},{navController.navigate(E11Routes.CONFLICTS)},DashboardDestination.DASHBOARD,{d->when(d){DashboardDestination.DASHBOARD->Unit;DashboardDestination.ORDERS->navController.navigate(E11Routes.ORDERS);DashboardDestination.PRODUCTS->navController.navigate(E11Routes.PRODUCTS);DashboardDestination.SETTINGS->navController.navigate(E11Routes.SETTINGS)}})}}
        composable(E11Routes.ORDERS){val s=activeStore?.let(::StoreId);if(s==null){LaunchedEffect(Unit){navController.navigate(E11Routes.CONNECTION)}}else{val vm=viewModel<OrdersViewModel>(factory=vmFactory{OrdersViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};OrdersScreen(mapOrdersState(st),{id->navController.navigate(E11Routes.order(id))},{vm.load(s)},{vm.nextPage(s)},{vm.load(s,it,true)})}}
        composable(E11Routes.ORDER_DETAIL,listOf(navArgument(E11Routes.ORDER_ID){type=NavType.StringType})){e->val s=activeStore?.let(::StoreId);val id=e.arguments?.getString(E11Routes.ORDER_ID);if(s!=null&&id!=null){val vm=viewModel<OrderDetailViewModel>(factory=vmFactory{OrderDetailViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s,id){vm.load(s,EntityId(id))};OrderDetailScreen(mapOrderDetailState(st),{vm.load(s,EntityId(id))},{navController.popBackStack()},{status->(st as? FeatureUiState.Success)?.value?.let{vm.update(s,it.copy(status=status))}})}}
        composable(E11Routes.PRODUCTS){val s=activeStore?.let(::StoreId);if(s==null){LaunchedEffect(Unit){navController.navigate(E11Routes.CONNECTION)}}else{val vm=viewModel<ProductsViewModel>(factory=vmFactory{ProductsViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};ProductsScreen(st,{id->navController.navigate(E11Routes.product(id))},{vm.load(s)},{vm.nextPage(s)},{vm.load(s,it,true)},{navController.navigate(E11Routes.PRODUCT_NEW)})}}
        composable(E11Routes.PRODUCT_DETAIL,listOf(navArgument(E11Routes.PRODUCT_ID){type=NavType.StringType})){e->val s=activeStore?.let(::StoreId);val id=e.arguments?.getString(E11Routes.PRODUCT_ID);if(s!=null&&id!=null){val vm=viewModel<ProductDetailViewModel>(factory=vmFactory{ProductDetailViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s,id){vm.load(s,EntityId(id))};when(val cur=st){is FeatureUiState.Success->ProductDetailScreen(cur.value,{navController.popBackStack()},{navController.navigate("products/$id/edit")},{navController.navigate(E11Routes.variations(id))});FeatureUiState.Loading,FeatureUiState.Pending->GlassScaffold{GlassLoading("در حال بارگذاری محصول…")};FeatureUiState.Empty->GlassScaffold{GlassEmptyState("محصول پیدا نشد.")};is FeatureUiState.Error->GlassScaffold{GlassErrorState(cur.message)};FeatureUiState.Offline->GlassScaffold{GlassOfflineState()};is FeatureUiState.Conflict->GlassScaffold{GlassErrorState("تعارض در داده‌های محصول وجود دارد.")}}}}
        composable(E11Routes.PRODUCT_NEW){val s=activeStore?.let(::StoreId);if(s!=null)ProductEditorRoute(dependencies,s,null,{navController.popBackStack()},{navController.popBackStack()})}
        composable("products/{productId}/edit",listOf(navArgument(E11Routes.PRODUCT_ID){type=NavType.StringType})){e->val s=activeStore?.let(::StoreId);val id=e.arguments?.getString(E11Routes.PRODUCT_ID);if(s!=null&&id!=null)ProductEditorRoute(dependencies,s,id,{navController.popBackStack()},{navController.popBackStack()})}
        composable(E11Routes.VARIATIONS,listOf(navArgument(E11Routes.PRODUCT_ID){type=NavType.StringType})){e->val s=activeStore?.let(::StoreId);val id=e.arguments?.getString(E11Routes.PRODUCT_ID);if(s!=null&&id!=null){val vm=viewModel<VariationsViewModel>(factory=vmFactory{VariationsViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s,id){vm.load(s,EntityId(id))};val list=(st as? FeatureUiState.Success)?.value.orEmpty().map{v->VariationUiModel(v.id.value,v.attributes.joinToString(" · "){a->"${a.name}: ${a.option}"},v.pricing.sale?:v.pricing.regular?:"—",v.stock?.quantity?.toString()?.removeSuffix(".0")?:"—")};VariationsManagementScreen(list,{navController.navigate("products/$id/variations/new")},{vid->navController.navigate("products/$id/variations/$vid/edit")})}}
        composable("products/{productId}/variations/new",listOf(navArgument(E11Routes.PRODUCT_ID){type=NavType.StringType})){e->val s=activeStore?.let(::StoreId);val id=e.arguments?.getString(E11Routes.PRODUCT_ID);if(s!=null&&id!=null)VariationEditorRoute(dependencies,s,id,null,{navController.popBackStack()},{navController.popBackStack()})}
        composable("products/{productId}/variations/{variationId}/edit",listOf(navArgument(E11Routes.PRODUCT_ID){type=NavType.StringType},navArgument("variationId"){type=NavType.StringType})){e->val s=activeStore?.let(::StoreId);val id=e.arguments?.getString(E11Routes.PRODUCT_ID);val vid=e.arguments?.getString("variationId");if(s!=null&&id!=null&&vid!=null)VariationEditorRoute(dependencies,s,id,vid,{navController.popBackStack()},{navController.popBackStack()})}
        composable(E11Routes.SYNC){val s=activeStore?.let(::StoreId);if(s!=null){val vm=viewModel<SyncViewModel>(factory=vmFactory{SyncViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};val ui=when(st){FeatureUiState.Loading->SyncUiState.Idle;FeatureUiState.Pending->SyncUiState.Running;is FeatureUiState.Success->SyncUiState.Success("همگام‌سازی با موفقیت انجام شد.");is FeatureUiState.Error->SyncUiState.Error(st.message);else->SyncUiState.Idle};SyncScreen(ui,{vm.sync(s)},{vm.sync(s)})}}
        composable(E11Routes.CONFLICTS){val s=activeStore?.let(::StoreId);if(s!=null){val vm=viewModel<ConflictsViewModel>(factory=vmFactory{ConflictsViewModel(dependencies)});val st by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};val list=(st as? FeatureUiState.Success)?.value.orEmpty().map{c->ConflictUiModel(c.id.value,"${c.entityType} · ${c.reason}",c.localSnapshot?:c.localVersion?.value?:"—",c.serverSnapshot?:c.remoteVersion?.value?:"—")};ConflictsScreen(list,{id->vm.resolve(s,EntityId(id),ConflictResolution.KEEP_LOCAL)},{id->vm.resolve(s,EntityId(id),ConflictResolution.KEEP_SERVER)})}}
        composable(E11Routes.SETTINGS){SettingsScreen(activeStore.orEmpty()){dependencies.onStoreDisconnected();activeStore=null;navController.navigate(E11Routes.CONNECTION){popUpTo(E11Routes.SETTINGS){inclusive=true}}}}
    }
}

@Composable private fun SettingsScreen(storeName:String,onDisconnect:()->Unit){GlassScaffold{padding->Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){GlassTopBar("تنظیمات","مدیریت اتصال و حساب فروشگاه");GlassCard{GlassText("فروشگاه متصل");GlassText(storeName,style=androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color=GlassTokens.muted))};GlassPrimaryAction("قطع اتصال",onDisconnect)}}}
private fun mapOrdersState(state:FeatureUiState<List<Order>>):OrdersUiState=when(state){FeatureUiState.Loading,FeatureUiState.Pending->OrdersUiState.Loading;FeatureUiState.Empty->OrdersUiState.Empty;is FeatureUiState.Error->OrdersUiState.Error(state.message,state.retryable);FeatureUiState.Offline->OrdersUiState.Offline();is FeatureUiState.Conflict->OrdersUiState.Error("تعارض در داده‌های سفارش وجود دارد.",false);is FeatureUiState.Success->OrdersUiState.Content(state.value.map{order->OrderRowUiModel(order.number,order.customer?.name.orEmpty(),order.customer?.email.orEmpty(),order.status.name,formatMoney(order.total),order.payment?.methodTitle.orEmpty(),order.modifiedAt?.toString().orEmpty())},state.value.size>=30)}
private fun mapOrderDetailState(state:FeatureUiState<Order>):OrderDetailUiState=when(state){FeatureUiState.Loading,FeatureUiState.Pending->OrderDetailUiState.Loading;FeatureUiState.Empty->OrderDetailUiState.NotFound;is FeatureUiState.Error->OrderDetailUiState.Error(state.message);FeatureUiState.Offline->OrderDetailUiState.Error("سفارش در حالت آفلاین در دسترس نیست.");is FeatureUiState.Conflict->OrderDetailUiState.Error("تعارض در داده‌های سفارش وجود دارد.");is FeatureUiState.Success->OrderDetailUiState.Content(state.value)}
private fun formatMoney(value:String?):String{val amount=value?.toDoubleOrNull()?:return "—";return "${java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply{maximumFractionDigits=0;minimumFractionDigits=0}.format(amount)} تومان"}
