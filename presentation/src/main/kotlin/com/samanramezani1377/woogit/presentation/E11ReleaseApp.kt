package com.samanramezani1377.woogit.presentation

import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import kotlinx.coroutines.launch

@Composable
fun E11ReleaseApp(d:V1PresentationDependencies, initialOrderId:String?=null){
    val nav=rememberNavController();var activeStore by rememberSaveable{mutableStateOf(d.initialStoreId)}
    val start=when{activeStore==null->"connection";initialOrderId!=null->"orders/$initialOrderId";else->"dashboard"}
    NavHost(nav,start){
        composable("connection"){ConnectionScreen(d){id->activeStore=id;nav.navigate("dashboard"){popUpTo("connection"){inclusive=true}}}}
        composable("dashboard"){activeStore?.let{s->DashboardScreen(d,StoreId(s),nav)}}
        composable("orders"){activeStore?.let{s->OrdersScreen(d,StoreId(s),nav)}}
        composable("orders/{orderId}",listOf(navArgument("orderId"){type=NavType.StringType})){b->activeStore?.let{s->OrderDetailScreen(d,StoreId(s),EntityId(b.arguments?.getString("orderId")!!))}}
        composable("products"){activeStore?.let{s->ProductsScreen(d,StoreId(s),nav)}}
        composable("products/new"){activeStore?.let{s->ProductEditorScreen(d,StoreId(s),EntityId("new"),nav)}}
        composable("products/{productId}",listOf(navArgument("productId"){type=NavType.StringType})){b->activeStore?.let{s->ProductEditorScreen(d,StoreId(s),EntityId(b.arguments?.getString("productId")!!),nav)}}
        composable("products/{productId}/variations",listOf(navArgument("productId"){type=NavType.StringType})){b->activeStore?.let{s->VariationsScreen(d,StoreId(s),EntityId(b.arguments?.getString("productId")!!),nav)}}
        composable("products/{productId}/variations/new",listOf(navArgument("productId"){type=NavType.StringType})){b->activeStore?.let{s->VariationEditorScreen(d,StoreId(s),EntityId(b.arguments?.getString("productId")!!),EntityId("new"),nav)}}
        composable("products/{productId}/variations/{variationId}",listOf(navArgument("productId"){type=NavType.StringType},navArgument("variationId"){type=NavType.StringType})){b->activeStore?.let{s->VariationEditorScreen(d,StoreId(s),EntityId(b.arguments?.getString("productId")!!),EntityId(b.arguments?.getString("variationId")!!),nav)}}
        composable("attributes"){activeStore?.let{s->AttributesScreen(d,StoreId(s),nav)}}
        composable("attributes/{attributeId}/terms",listOf(navArgument("attributeId"){type=NavType.StringType})){b->activeStore?.let{s->TermsScreen(d,StoreId(s),EntityId(b.arguments?.getString("attributeId")!!))}}
        composable("sync"){activeStore?.let{s->SyncScreen(d,StoreId(s))}}
        composable("conflicts"){activeStore?.let{s->ConflictsScreen(d,StoreId(s))}}
        composable("settings"){activeStore?.let{s->SettingsScreen(d,StoreId(s),nav){activeStore=null}}}
    }
}

@Composable private fun ConnectionScreen(d:V1PresentationDependencies,onConnected:(String)->Unit){val vm=viewModel<ConnectionViewModel>(factory=vmFactory{ConnectionViewModel(d)});val state by vm.state.collectAsState();var url by rememberSaveable{mutableStateOf("")};var key by rememberSaveable{mutableStateOf("")};var secret by remember{mutableStateOf("")};LaunchedEffect(state){val x=state as? FeatureUiState.Success<StoreConnection>;if(x!=null){secret="";onConnected(x.value.storeId.value)}};GlassScaffold{p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){GlassTopBar("اتصال فروشگاه","WooCommerce");GlassTextField(url,{url=it},"آدرس فروشگاه HTTPS");GlassTextField(key,{key=it},"Consumer Key");GlassPasswordField(secret,{secret=it});when(val x=state){FeatureUiState.Loading->GlassLoading("در حال اتصال…");is FeatureUiState.Error->GlassErrorState(x.message);else->Unit};GlassPrimaryAction("بررسی و اتصال",{vm.connect(url,key,secret)},Modifier.fillMaxWidth(),url.isNotBlank()&&key.isNotBlank()&&secret.isNotBlank())}}}

@Composable private fun DashboardScreen(d:V1PresentationDependencies,s:StoreId,nav:NavHostController){val vm=viewModel<SyncViewModel>(factory=vmFactory{SyncViewModel(d)});val state by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};GlassScaffold{p->Column(Modifier.padding(p).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){GlassTopBar("داشبورد",s.value);GlassStatusBadge("Connected");when(val x=state){is FeatureUiState.Success<*>->GlassCard{val metadata=x.value as SyncMetadata;GlassText("وضعیت همگام‌سازی: ${metadata.state}");GlassText("در انتظار: ${metadata.pending.size}")};FeatureUiState.Loading->GlassLoading();is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s)};else->Unit};GlassButton("سفارش‌ها",{nav.navigate("orders")},Modifier.fillMaxWidth());GlassButton("محصولات",{nav.navigate("products")},Modifier.fillMaxWidth());GlassButton("ویژگی‌ها",{nav.navigate("attributes")},Modifier.fillMaxWidth());GlassButton("همگام‌سازی",{nav.navigate("sync")},Modifier.fillMaxWidth());GlassButton("تعارض‌ها",{nav.navigate("conflicts")},Modifier.fillMaxWidth());GlassSecondaryButton("تنظیمات",{nav.navigate("settings")},Modifier.fillMaxWidth())}}}

@Composable private fun OrdersScreen(d:V1PresentationDependencies,s:StoreId,nav:NavHostController){val vm=viewModel<OrdersViewModel>(factory=vmFactory{OrdersViewModel(d)});val state by vm.state.collectAsState();var q by rememberSaveable{mutableStateOf("")};LaunchedEffect(s){vm.load(s)};LaunchedEffect(q){vm.load(s,q)};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("سفارش‌ها");GlassSearchField(q,{q=it},onClear={q=""});when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("سفارشی وجود ندارد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s,q)};FeatureUiState.Offline->GlassOfflineState();is FeatureUiState.Success<*>->LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){val orders=x.value as List<Order>;items(orders){o->GlassListItem("#${o.number}","${o.status} • ${o.total.orEmpty()} ${o.currency.orEmpty()}",onClick={nav.navigate("orders/${o.id.value}")},trailing={GlassStatusBadge(o.status.name)})};item{GlassSecondaryButton("صفحه بعد",{vm.nextPage(s)},Modifier.fillMaxWidth())}};else->Unit}}}}

@Composable private fun OrderDetailScreen(d:V1PresentationDependencies,s:StoreId,id:EntityId){val vm=viewModel<OrderDetailViewModel>(factory=vmFactory{OrderDetailViewModel(d)});val state by vm.state.collectAsState();var note by rememberSaveable{mutableStateOf("")};LaunchedEffect(s,id){vm.load(s,id)};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("سفارش پیدا نشد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s,id)};FeatureUiState.Pending->GlassPendingState();is FeatureUiState.Success<*>->{val o=x.value as Order;GlassTopBar("سفارش #${o.number}",o.modifiedAt?.toString());GlassCard{GlassText("وضعیت: ${o.status}");GlassText("مبلغ: ${o.total.orEmpty()} ${o.currency.orEmpty()}");GlassText("پرداخت: ${o.payment?.methodTitle.orEmpty()}");GlassText("تراکنش: ${o.payment?.transactionId.orEmpty()}")};GlassSection("مشتری"){GlassCard{GlassText(o.customer?.name.orEmpty());GlassText(o.customer?.email.orEmpty())}};GlassSection("آدرس صورتحساب"){GlassCard{GlassText(o.billing?.let{listOfNotNull(it.firstName,it.lastName,it.address1,it.city,it.postcode,it.country).joinToString(" ")}.orEmpty())}};GlassSection("اقلام"){o.items.forEach{GlassListItem(it.name,"${it.quantity} × ${it.total}")}};GlassSection("حمل و تخفیف"){o.shippingLines.forEach{GlassListItem(it.methodTitle.orEmpty(),it.total.orEmpty())};o.discounts.forEach{GlassListItem(it.code,it.total)}};GlassDropdown("وضعیت",o.status,listOf(OrderStatus.PENDING,OrderStatus.PROCESSING,OrderStatus.ON_HOLD,OrderStatus.COMPLETED,OrderStatus.CANCELLED,OrderStatus.REFUNDED,OrderStatus.FAILED),{it.name},{vm.update(s,o.copy(status=it))});GlassTextField(note,{note=it},"یادداشت",singleLine=false);GlassButton("افزودن یادداشت",{vm.note(s,id,note);note=""},Modifier.fillMaxWidth(),note.isNotBlank())};else->Unit}}}}

@Composable private fun ProductsScreen(d:V1PresentationDependencies,s:StoreId,nav:NavHostController){val vm=viewModel<ProductsViewModel>(factory=vmFactory{ProductsViewModel(d)});val state by vm.state.collectAsState();var q by rememberSaveable{mutableStateOf("")};val listState=rememberLazyListState();LaunchedEffect(s,q){vm.load(s,q)};LaunchedEffect(listState){snapshotFlow{Pair(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1,listState.layoutInfo.totalItemsCount)}.collect{(last,total)->if(last>=0&&total>0&&last>=total-5)vm.nextPage(s)}};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("محصولات");GlassSearchField(q,{q=it},onClear={q=""});GlassButton("افزودن محصول",{nav.navigate("products/new")},Modifier.fillMaxWidth());when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("محصولی وجود ندارد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s,q)};is FeatureUiState.Success<*>->{val products=x.value.filterIsInstance<Product>();LazyColumn(state=listState,verticalArrangement=Arrangement.spacedBy(8.dp)){items(products,key={it.id.value}){product->GlassListItem(product.name,"${product.type.name} • SKU ${product.sku.orEmpty()}",onClick={nav.navigate("products/${product.id.value}")},trailing={GlassStatusBadge(product.status.name)})};item(key="product-pagination-loading"){if(products.size>=30)GlassLoading("در حال دریافت محصولات…")}}};else->Unit}}}}

@Composable private fun ProductEditorScreen(d:V1PresentationDependencies,s:StoreId,id:EntityId,nav:NavHostController){
    val vm=viewModel<ProductDetailViewModel>(factory=vmFactory{ProductDetailViewModel(d)})
    val create=id.value=="new"
    val state by vm.state.collectAsState()
    val categoryState by vm.categories.collectAsState()
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var name by rememberSaveable(id.value){mutableStateOf("")}
    var sku by rememberSaveable(id.value){mutableStateOf("")}
    var description by rememberSaveable(id.value){mutableStateOf("")}
    var shortDescription by rememberSaveable(id.value){mutableStateOf("")}
    var regular by rememberSaveable(id.value){mutableStateOf("")}
    var sale by rememberSaveable(id.value){mutableStateOf("")}
    var type by rememberSaveable(id.value){mutableStateOf(ProductType.SIMPLE)}
    var status by rememberSaveable(id.value){mutableStateOf(ProductStatus.DRAFT)}
    var stock by rememberSaveable(id.value){mutableStateOf("")}
    var stockStatus by rememberSaveable(id.value){mutableStateOf(StockStatus.IN_STOCK)}
    var manageStock by rememberSaveable(id.value){mutableStateOf(false)}
    var images by remember(id.value){mutableStateOf(emptyList<ProductImage>())}
    var categories by remember(id.value){mutableStateOf(emptyList<IdName>())}
    var mediaBusy by remember{mutableStateOf(false)}
    var mediaError by remember{mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->
        if(uri!=null&&!mediaBusy){scope.launch{
            mediaBusy=true;mediaError=null
            val result=runCatching{
                val resolver=context.contentResolver
                val bytes=resolver.openInputStream(uri)?.use{it.readBytes()}?:error("فایل تصویر قابل خواندن نیست.")
                if(bytes.size>15*1024*1024)error("حجم تصویر بیشتر از 15 مگابایت است.")
                val mime=resolver.getType(uri)?:"image/jpeg"
                val fileName=uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank{"product-${System.currentTimeMillis()}.jpg"}
                d.uploadMedia(s,fileName,bytes,mime)
            }.getOrElse{CoreResult.Failure(com.samanramezani1377.woogit.core.domain.error.DomainError.Network(it.message?:"آپلود تصویر ناموفق بود."))}
            when(result){is CoreResult.Success->{images=images+result.value};is CoreResult.Failure->{mediaError=PresentationErrorMapper.message(result.error)}}
            mediaBusy=false
        }}
    }
    LaunchedEffect(s,id){if(create)vm.loadCategories(s)else vm.load(s,id)}
    LaunchedEffect(state){(state as? FeatureUiState.Success<Product>)?.value?.let{p->
        name=p.name;sku=p.sku.orEmpty();description=p.description.orEmpty();shortDescription=p.shortDescription.orEmpty();regular=p.pricing.regular.orEmpty();sale=p.pricing.sale.orEmpty();type=p.type;status=p.status;stock=p.stock?.quantity?.toString().orEmpty();stockStatus=p.stock?.status?:StockStatus.IN_STOCK;manageStock=p.stock?.manageStock?:false;images=p.images;categories=p.categories
    }}
    val availableCategories=(categoryState as? FeatureUiState.Success<List<IdName>>)?.value.orEmpty()
    GlassScaffold{padding->Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        GlassTopBar(if(create)"افزودن محصول" else "ویرایش محصول")
        GlassText("تصاویر محصول")
        if(images.isEmpty())GlassEmptyState("هنوز تصویری اضافه نشده") else images.forEachIndexed{index,image->GlassCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){AsyncImage(model=image.src,contentDescription=image.alt?:image.name,modifier=Modifier.size(88.dp),contentScale=ContentScale.Crop);Column(Modifier.weight(1f)){GlassText(if(index==0)"تصویر اصلی" else "تصویر ${index+1}");GlassText(image.name.orEmpty());if(index>0)GlassSecondaryButton("قرار دادن به عنوان اصلی",{images=listOf(image)+images.filterIndexed{i,_->i!=index}},Modifier.fillMaxWidth())};GlassDestructiveAction("حذف از محصول",{images=images.filterIndexed{i,_->i!=index}},Modifier.wrapContentWidth())}}}
        GlassPrimaryAction(if(mediaBusy)"در حال آپلود…" else "افزودن تصویر",{picker.launch("image/*")},Modifier.fillMaxWidth(),!mediaBusy)
        mediaError?.let{GlassErrorState(it)}
        GlassTextField(name,{name=it},"نام")
        GlassTextField(sku,{sku=it},"SKU")
        GlassTextField(description,{description=it},"توضیحات",singleLine=false)
        ProductHtmlPreview("پیش‌نمایش توضیحات",description)
        GlassTextField(shortDescription,{shortDescription=it},"توضیح کوتاه",singleLine=false)
        ProductHtmlPreview("پیش‌نمایش توضیح کوتاه",shortDescription)
        GlassText("دسته‌بندی‌ها")
        when(categoryState){FeatureUiState.Loading->GlassLoading("در حال دریافت دسته‌بندی‌ها…");is FeatureUiState.Error->GlassErrorState(categoryState.message){vm.loadCategories(s)};else->Unit}
        availableCategories.forEach{category->GlassCheckbox(categories.any{it.id==category.id},{checked->categories=if(checked)(categories+category).distinctBy{it.id.value}else categories.filterNot{it.id==category.id}},category.name)}
        if(categories.isNotEmpty())GlassText("انتخاب‌شده: ${categories.joinToString("، "){it.name}}")
        GlassTextField(regular,{regular=it},"قیمت عادی")
        GlassTextField(sale,{sale=it},"قیمت ویژه")
        GlassTextField(stock,{stock=it},"موجودی")
        GlassDropdown("وضعیت موجودی",stockStatus,StockStatus.entries,{it.name},{stockStatus=it})
        GlassCheckbox(manageStock,{manageStock=it},"مدیریت موجودی")
        GlassDropdown("نوع",type,ProductType.entries,{it.name},{type=it})
        GlassDropdown("وضعیت",status,ProductStatus.entries,{it.name},{status=it})
        GlassButton("ذخیره",{
            val current=(state as? FeatureUiState.Success<Product>)?.value
            val product=Product(current?.id?:EntityId("local-product-${System.currentTimeMillis()}"),name,sku,description,shortDescription,status,type,Pricing(regular.ifBlank{null},sale.ifBlank{null},sale.isNotBlank()),Stock(stock.toDoubleOrNull(),stockStatus,manageStock),images,categories,current?.attributes.orEmpty(),current?.modifiedAt)
            vm.save(s,product,create){nav.popBackStack()}
        },Modifier.fillMaxWidth(),name.isNotBlank()&&!mediaBusy)
        if(!create&&state is FeatureUiState.Success){GlassButton("تغییرپذیرها",{nav.navigate("products/${id.value}/variations")},Modifier.fillMaxWidth());GlassDestructiveAction("حذف محصول",{vm.delete(s,id){nav.popBackStack()}},Modifier.fillMaxWidth())}
        if(state is FeatureUiState.Pending)GlassPendingState()
        if(state is FeatureUiState.Error)GlassErrorState((state as FeatureUiState.Error).message)
    }}
}

@Composable private fun ProductHtmlPreview(title:String,html:String){if(html.isBlank())return;GlassSection(title){AndroidView(modifier=Modifier.fillMaxWidth(),factory={context->TextView(context).apply{setTextIsSelectable(true);movementMethod=LinkMovementMethod.getInstance();setPadding(4,4,4,4)}},update={view->view.text=Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY)}}}

@Composable private fun VariationsScreen(d:V1PresentationDependencies,s:StoreId,p:EntityId,nav:NavHostController){val vm=viewModel<VariationsViewModel>(factory=vmFactory{VariationsViewModel(d)});val state by vm.state.collectAsState();LaunchedEffect(s,p){vm.load(s,p)};GlassScaffold{pad->Column(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("تغییرپذیرها");GlassButton("افزودن تغییرپذیر",{nav.navigate("products/${p.value}/variations/new")},Modifier.fillMaxWidth());when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("تغییرپذیری وجود ندارد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s,p)};is FeatureUiState.Success<*>->LazyColumn{val variations=x.value as List<Variation>;items(variations){v->GlassListItem(v.sku.orEmpty(),"${v.pricing.regular.orEmpty()} • ${v.attributes.joinToString(" / "){a->"${a.name}: ${a.option}"}}",onClick={nav.navigate("products/${p.value}/variations/${v.id.value}")})}};else->Unit}}}}

@Composable private fun VariationEditorScreen(d:V1PresentationDependencies,s:StoreId,p:EntityId,id:EntityId,nav:NavHostController){val vm=viewModel<VariationEditorViewModel>(factory=vmFactory{VariationEditorViewModel(d)});val state by vm.state.collectAsState();val create=id.value=="new";LaunchedEffect(s,p,id){if(!create)vm.load(s,p,id)};var sku by rememberSaveable(id.value){mutableStateOf("")};var regular by rememberSaveable(id.value){mutableStateOf("")};var sale by rememberSaveable(id.value){mutableStateOf("")};var an by rememberSaveable(id.value+"a"){mutableStateOf("")};var ao by rememberSaveable(id.value+"o"){mutableStateOf("")};LaunchedEffect(state){(state as? FeatureUiState.Success<Variation>)?.value?.let{v->sku=v.sku.orEmpty();regular=v.pricing.regular.orEmpty();sale=v.pricing.sale.orEmpty();an=v.attributes.firstOrNull()?.name.orEmpty();ao=v.attributes.firstOrNull()?.option.orEmpty()}};GlassScaffold{pad->Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar(if(create)"افزودن تغییرپذیر" else "ویرایش تغییرپذیر");GlassTextField(sku,{sku=it},"SKU");GlassTextField(regular,{regular=it},"قیمت عادی");GlassTextField(sale,{sale=it},"قیمت ویژه");GlassTextField(an,{an=it},"ویژگی");GlassTextField(ao,{ao=it},"مقدار");GlassButton("ذخیره",{val current=(state as? FeatureUiState.Success<Variation>)?.value;val v=Variation(current?.id?:EntityId("local-variation-${System.currentTimeMillis()}"),p,if(an.isBlank()) emptyList() else listOf(VariationAttribute(an,ao)),Pricing(regular.ifBlank{null},sale.ifBlank{null},sale.isNotBlank()),current?.stock,sku,current?.image);vm.save(s,p,id,v,create){nav.popBackStack()}},Modifier.fillMaxWidth());if(state is FeatureUiState.Pending)GlassPendingState();if(state is FeatureUiState.Error)GlassErrorState((state as FeatureUiState.Error).message)}}}

@Composable private fun AttributesScreen(d:V1PresentationDependencies,s:StoreId,nav:NavHostController){val vm=viewModel<AttributesViewModel>(factory=vmFactory{AttributesViewModel(d)});val state by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};var name by rememberSaveable{mutableStateOf("")};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("ویژگی‌ها");GlassTextField(name,{name=it},"نام ویژگی");GlassButton("افزودن ویژگی",{vm.add(s,GlobalAttribute(EntityId("local-attribute-${System.currentTimeMillis()}"),name,"",emptyList()))},Modifier.fillMaxWidth(),name.isNotBlank());when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("ویژگی‌ای وجود ندارد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s)};is FeatureUiState.Success<*>->LazyColumn{val attrs=x.value as List<GlobalAttribute>;items(attrs){a->GlassListItem(a.name,"",onClick={nav.navigate("attributes/${a.id.value}/terms")})}};else->Unit}}}}

@Composable private fun TermsScreen(d:V1PresentationDependencies,s:StoreId,a:EntityId){val vm=viewModel<TermsViewModel>(factory=vmFactory{TermsViewModel(d)});val state by vm.state.collectAsState();LaunchedEffect(s,a){vm.load(s,a)};var name by rememberSaveable{mutableStateOf("")};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("مقادیر ویژگی");GlassTextField(name,{name=it},"نام مقدار");GlassButton("افزودن مقدار",{vm.add(s,a,AttributeTerm(EntityId("local-term-${System.currentTimeMillis()}"),name,null))},Modifier.fillMaxWidth(),name.isNotBlank());when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("مقداری وجود ندارد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s,a)};is FeatureUiState.Success<*>->LazyColumn{val terms=x.value as List<AttributeTerm>;items(terms){t->GlassListItem(t.name,"")}};else->Unit}}}}

@Composable private fun SyncScreen(d:V1PresentationDependencies,s:StoreId){val vm=viewModel<SyncViewModel>(factory=vmFactory{SyncViewModel(d)});val state by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("همگام‌سازی");when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Pending->GlassPendingState();is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s)};is FeatureUiState.Success<*>->{val m=x.value as SyncMetadata;GlassCard{GlassText("وضعیت: ${m.state}");GlassText("آخرین همگام‌سازی: ${m.version?.modifiedAt}");GlassText("در انتظار: ${m.pending.size}")}};else->Unit};GlassButton("همگام‌سازی عملیات در انتظار",{vm.sync(s)},Modifier.fillMaxWidth())}}}

@Composable private fun ConflictsScreen(d:V1PresentationDependencies,s:StoreId){val vm=viewModel<ConflictsViewModel>(factory=vmFactory{ConflictsViewModel(d)});val state by vm.state.collectAsState();LaunchedEffect(s){vm.load(s)};GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("تعارض‌ها");when(val x=state){FeatureUiState.Loading->GlassLoading();FeatureUiState.Empty->GlassEmptyState("تعارضی وجود ندارد");is FeatureUiState.Error->GlassErrorState(x.message){vm.load(s)};is FeatureUiState.Success<*>->LazyColumn{val conflicts=x.value as List<Conflict>;items(conflicts){c->GlassListItem("${c.entityType} ${c.entityId.value}",c.reason)}};else->Unit}}}}

@Composable private fun SettingsScreen(d:V1PresentationDependencies,s:StoreId,nav:NavHostController,onDisconnected:()->Unit){GlassScaffold{p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){GlassTopBar("تنظیمات");GlassText("فروشگاه: ${s.value}");GlassButton("همگام‌سازی",{nav.navigate("sync")},Modifier.fillMaxWidth());GlassDestructiveAction("قطع اتصال",{d.onStoreDisconnected();onDisconnected()},Modifier.fillMaxWidth())}}}

private fun <T:ViewModel> vmFactory(block:()->T)=object:androidx.lifecycle.ViewModelProvider.Factory{override fun <VM:ViewModel> create(modelClass:Class<VM>):VM=block() as VM}
