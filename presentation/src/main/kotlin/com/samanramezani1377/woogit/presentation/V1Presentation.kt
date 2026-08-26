package com.samanramezani1377.woogit.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.core.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class V1PresentationDependencies(
    val getStore:GetStore,val connectStore:ConnectStore,val disconnectStore:DisconnectStore,val getOrders:GetOrders,val getOrder:GetOrder,val updateOrder:UpdateOrder,val addOrderNote:AddOrderNote,
    val getProducts:GetProducts,val getProduct:GetProduct,val createProduct:CreateProduct,val updateProduct:UpdateProduct,val deleteProduct:DeleteProduct,
    val getVariations:GetVariations,val getVariation:GetVariation,val createVariation:CreateVariation,val updateVariation:UpdateVariation,val deleteVariation:DeleteVariation,
    val getAttributes:GetAttributes,val getAttribute:GetAttribute,val createAttribute:CreateAttribute,val updateAttribute:UpdateAttribute,val deleteAttribute:DeleteAttribute,
    val getTerms:GetTerms,val getTerm:GetTerm,val createTerm:CreateTerm,val updateTerm:UpdateTerm,val deleteTerm:DeleteTerm,val uploadMedia:UploadMedia,val deleteMedia:DeleteMedia,
    val getConnectionState:GetConnectionState,val getSyncState:GetSyncState,val getPending:GetPendingOperations,val getConflicts:suspend(StoreId)->CoreResult<List<Conflict>>,val resolveConflict:suspend(StoreId,EntityId,ConflictResolution)->CoreResult<Unit>,val syncPending:SyncPendingOperations,
    val initialStoreId:String?,val onStoreConnected:(String)->Unit,val onStoreDisconnected:()->Unit,
)

sealed interface FeatureUiState<out T>{
    data object Loading:FeatureUiState<Nothing>;data object Empty:FeatureUiState<Nothing>;data class Success<T>(val value:T):FeatureUiState<T>
    data class Error(val message:String,val retryable:Boolean=true):FeatureUiState<Nothing>;data object Offline:FeatureUiState<Nothing>;data object Pending:FeatureUiState<Nothing>;data class Conflict(val count:Int=1):FeatureUiState<Nothing>
}
private fun CoreResult.Failure.toUi()=FeatureUiState.Error(PresentationErrorMapper.message(error),error.recoverable)

class ConnectionViewModel(private val d:V1PresentationDependencies):ViewModel(){
    private val _state=MutableStateFlow<FeatureUiState<StoreConnection>>(FeatureUiState.Empty);val state:StateFlow<FeatureUiState<StoreConnection>>=_state.asStateFlow()
    fun connect(url:String,key:String,secret:String)=viewModelScope.launch{_state.value=FeatureUiState.Loading;val u=url.trim().trimEnd('/');if(!u.startsWith("https://")){_state.value=FeatureUiState.Error("آدرس فروشگاه باید با HTTPS شروع شود.",false);return@launch};when(val r=d.connectStore(StoreConnection(StoreId("store-${u.lowercase().hashCode().toUInt().toString(16)}"),u,ConnectionState.DISCONNECTED,null),key,secret)){is CoreResult.Success->{_state.value=FeatureUiState.Success(r.value);d.onStoreConnected(r.value.storeId.value)};is CoreResult.Failure->_state.value=r.toUi()}}
}
class OrdersViewModel(private val d:V1PresentationDependencies):ViewModel(){
    private val _state=MutableStateFlow<FeatureUiState<List<Order>>>(FeatureUiState.Loading);val state=_state.asStateFlow();var page=1;private var query="";private var job:Job?=null
    fun load(s:StoreId,search:String="",reset:Boolean=true){query=search;if(reset)page=1;job?.cancel();job=viewModelScope.launch{delay(350);_state.value=FeatureUiState.Loading;when(val r=d.getOrders(s,page,30,query.ifBlank{null},null)){is CoreResult.Success->_state.value=if(r.value.isEmpty())FeatureUiState.Empty else FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}}}
    fun nextPage(s:StoreId)=viewModelScope.launch{val target=page+1;when(val r=d.getOrders(s,target,30,query.ifBlank{null},null)){is CoreResult.Success->{page=target;val old=(_state.value as? FeatureUiState.Success)?.value.orEmpty();_state.value=FeatureUiState.Success(old+r.value)};is CoreResult.Failure->_state.value=r.toUi()}}
}
class OrderDetailViewModel(private val d:V1PresentationDependencies):ViewModel(){
    private val _state=MutableStateFlow<FeatureUiState<Order>>(FeatureUiState.Loading);val state=_state.asStateFlow()
    fun load(s:StoreId,id:EntityId)=viewModelScope.launch{when(val r=d.getOrder(s,id)){is CoreResult.Success->_state.value=FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}}
    fun update(s:StoreId,o:Order)=viewModelScope.launch{_state.value=FeatureUiState.Pending;when(val r=d.updateOrder(s,o.id,o)){is CoreResult.Success->_state.value=FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}}
    fun note(s:StoreId,id:EntityId,text:String)=viewModelScope.launch{when(d.addOrderNote(s,id,text)){is CoreResult.Success->Unit;is CoreResult.Failure->_state.value=FeatureUiState.Error("افزودن یادداشت ناموفق بود.")}}
}
class ProductsViewModel(private val d:V1PresentationDependencies):ViewModel(){
    private val _state=MutableStateFlow<FeatureUiState<List<Product>>>(FeatureUiState.Loading);val state=_state.asStateFlow();var page=1;private var query="";private var job:Job?=null
    fun load(s:StoreId,search:String="",reset:Boolean=true){query=search;if(reset)page=1;job?.cancel();job=viewModelScope.launch{delay(350);_state.value=FeatureUiState.Loading;when(val r=d.getProducts(s,page,30,query.ifBlank{null})){is CoreResult.Success->_state.value=if(r.value.isEmpty())FeatureUiState.Empty else FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}}}
    fun nextPage(s:StoreId)=viewModelScope.launch{val target=page+1;when(val r=d.getProducts(s,target,30,query.ifBlank{null})){is CoreResult.Success->{page=target;val old=(_state.value as? FeatureUiState.Success)?.value.orEmpty();_state.value=FeatureUiState.Success(old+r.value)};is CoreResult.Failure->_state.value=r.toUi()}}
}
class ProductDetailViewModel(private val d:V1PresentationDependencies):ViewModel(){
    private val _state=MutableStateFlow<FeatureUiState<Product>>(FeatureUiState.Loading);val state=_state.asStateFlow()
    fun load(s:StoreId,id:EntityId)=viewModelScope.launch{when(val r=d.getProduct(s,id)){is CoreResult.Success->_state.value=FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}}
    fun save(s:StoreId,p:Product,create:Boolean,done:()->Unit)=viewModelScope.launch{_state.value=FeatureUiState.Pending;when(val r=if(create)d.createProduct(s,p)else d.updateProduct(s,p.id,p)){is CoreResult.Success->{_state.value=FeatureUiState.Success(r.value);done()};is CoreResult.Failure->_state.value=r.toUi()}}
    fun delete(s:StoreId,id:EntityId,done:()->Unit)=viewModelScope.launch{_state.value=FeatureUiState.Pending;when(val r=d.deleteProduct(s,id)){is CoreResult.Success->{_state.value=FeatureUiState.Empty;done()};is CoreResult.Failure->_state.value=r.toUi()}}
}
class VariationsViewModel(private val d:V1PresentationDependencies):ViewModel(){private val _state=MutableStateFlow<FeatureUiState<List<Variation>>>(FeatureUiState.Loading);val state=_state.asStateFlow();fun load(s:StoreId,p:EntityId,page:Int=1)=viewModelScope.launch{when(val r=d.getVariations(s,p,page,30)){is CoreResult.Success->_state.value=if(r.value.isEmpty())FeatureUiState.Empty else FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}}}
class VariationEditorViewModel(private val d:V1PresentationDependencies):ViewModel(){private val _state=MutableStateFlow<FeatureUiState<Variation>>(FeatureUiState.Loading);val state=_state.asStateFlow();fun load(s:StoreId,p:EntityId,id:EntityId)=viewModelScope.launch{when(val r=d.getVariation(s,p,id)){is CoreResult.Success->_state.value=FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}};fun save(s:StoreId,p:EntityId,id:EntityId,v:Variation,create:Boolean,done:()->Unit)=viewModelScope.launch{_state.value=FeatureUiState.Pending;when(val r=if(create)d.createVariation(s,v)else d.updateVariation(s,p,id,v)){is CoreResult.Success->{_state.value=FeatureUiState.Success(r.value);done()};is CoreResult.Failure->_state.value=r.toUi()}}}
class AttributesViewModel(private val d:V1PresentationDependencies):ViewModel(){private val _state=MutableStateFlow<FeatureUiState<List<GlobalAttribute>>>(FeatureUiState.Loading);val state=_state.asStateFlow();fun load(s:StoreId)=viewModelScope.launch{when(val r=d.getAttributes(s,1,100)){is CoreResult.Success->_state.value=if(r.value.isEmpty())FeatureUiState.Empty else FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}};fun add(s:StoreId,v:GlobalAttribute)=viewModelScope.launch{when(val r=d.createAttribute(s,v)){is CoreResult.Success->load(s);is CoreResult.Failure->_state.value=r.toUi()}}}
class TermsViewModel(private val d:V1PresentationDependencies):ViewModel(){private val _state=MutableStateFlow<FeatureUiState<List<AttributeTerm>>>(FeatureUiState.Loading);val state=_state.asStateFlow();fun load(s:StoreId,a:EntityId)=viewModelScope.launch{when(val r=d.getTerms(s,a,1,100)){is CoreResult.Success->_state.value=if(r.value.isEmpty())FeatureUiState.Empty else FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}};fun add(s:StoreId,a:EntityId,v:AttributeTerm)=viewModelScope.launch{when(val r=d.createTerm(s,a,v)){is CoreResult.Success->load(s,a);is CoreResult.Failure->_state.value=r.toUi()}}}
class SyncViewModel(private val d:V1PresentationDependencies):ViewModel(){private val _state=MutableStateFlow<FeatureUiState<SyncMetadata>>(FeatureUiState.Loading);val state=_state.asStateFlow();fun load(s:StoreId)=viewModelScope.launch{when(val r=d.getSyncState(s)){is CoreResult.Success->_state.value=FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}};fun sync(s:StoreId)=viewModelScope.launch{_state.value=FeatureUiState.Pending;when(d.syncPending(s)){is CoreResult.Success->load(s);is CoreResult.Failure->_state.value=FeatureUiState.Error("همگام‌سازی ناموفق بود.")}}}
class ConflictsViewModel(private val d:V1PresentationDependencies):ViewModel(){private val _state=MutableStateFlow<FeatureUiState<List<Conflict>>>(FeatureUiState.Loading);val state=_state.asStateFlow();fun load(s:StoreId)=viewModelScope.launch{when(val r=d.getConflicts(s)){is CoreResult.Success->_state.value=if(r.value.isEmpty())FeatureUiState.Empty else FeatureUiState.Success(r.value);is CoreResult.Failure->_state.value=r.toUi()}};fun resolve(s:StoreId,id:EntityId,r:ConflictResolution)=viewModelScope.launch{when(d.resolveConflict(s,id,r)){is CoreResult.Success->load(s);is CoreResult.Failure->_state.value=FeatureUiState.Error("حل تعارض ناموفق بود.")}}}
