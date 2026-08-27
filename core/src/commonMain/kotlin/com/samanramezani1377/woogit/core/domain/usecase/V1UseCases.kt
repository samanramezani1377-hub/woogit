package com.samanramezani1377.woogit.core.domain.usecase

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.repository.*
import com.samanramezani1377.woogit.core.domain.model.*

class GetOrderUseCase(private val r: OrderRepository) : GetOrder { override suspend fun invoke(storeId: StoreId, id: EntityId) = r.get(storeId, id) }
class GetOrdersUseCase(private val r: OrderRepository) : GetOrders { override suspend fun invoke(storeId: StoreId, page: Int, perPage: Int, search: String?, status: String?) = r.list(storeId, page, perPage, search, status) }
class UpdateOrderUseCase(private val r: OrderRepository) : UpdateOrder { override suspend fun invoke(storeId: StoreId, id: EntityId, value: Order) = r.update(storeId, id, value) }
class GetProductUseCase(private val r: ProductRepository) : GetProduct { override suspend fun invoke(storeId: StoreId, id: EntityId) = r.get(storeId, id) }
class GetProductsUseCase(private val r: ProductRepository) : GetProducts { override suspend fun invoke(storeId: StoreId, page: Int, perPage: Int, search: String?) = r.list(storeId, page, perPage, search) }
class CreateProductUseCase(private val r: ProductRepository) : CreateProduct { override suspend fun invoke(storeId: StoreId, value: Product) = r.create(storeId, value) }
class UpdateProductUseCase(private val r: ProductRepository) : UpdateProduct { override suspend fun invoke(storeId: StoreId, id: EntityId, value: Product) = r.update(storeId, id, value) }
class DeleteProductUseCase(private val r: ProductRepository) : DeleteProduct { override suspend fun invoke(storeId: StoreId, id: EntityId) = r.delete(storeId, id) }
class UploadMediaUseCase(private val r: MediaRepository) : UploadMedia { override suspend fun invoke(storeId: StoreId, fileName: String, bytes: ByteArray, mediaType: String) = r.upload(storeId, fileName, bytes, mediaType) }
class DeleteMediaUseCase(private val r: MediaRepository) : DeleteMedia { override suspend fun invoke(storeId: StoreId, mediaId: EntityId) = r.delete(storeId, mediaId) }
class GetStoreUseCase(private val r: StoreRepository) : GetStore { override suspend fun invoke(storeId: StoreId) = r.get(storeId) }
class ConnectStoreUseCase(private val r: StoreRepository) : ConnectStore { override suspend fun invoke(store: StoreConnection, consumerKey: String, consumerSecret: String) = r.connect(store, consumerKey, consumerSecret) }
class DisconnectStoreUseCase(private val r: StoreRepository) : DisconnectStore { override suspend fun invoke(storeId: StoreId) = r.disconnect(storeId) }
class GetConnectionStateUseCase(private val r: StoreRepository) : GetConnectionState {
    override suspend fun invoke(storeId: StoreId) = r.get(storeId).let { result ->
        if (result is CoreResult.Success) CoreResult.Success(result.value.state)
        else CoreResult.Failure((result as CoreResult.Failure).error)
    }
}
class GetPendingOperationsUseCase(private val r: PendingOperationRepository) : GetPendingOperations { override suspend fun invoke(storeId: StoreId) = r.getPending(storeId) }
class EnqueueOperationUseCase(private val r: PendingOperationRepository) : EnqueueOperation { override suspend fun invoke(operation: PendingOperation) = r.enqueue(operation) }
class GetSyncStateUseCase(private val r: SyncRepository) : GetSyncState { override suspend fun invoke(storeId: StoreId) = r.getState(storeId) }
class SyncPendingOperationsUseCase(private val r: SyncRepository) : SyncPendingOperations { override suspend fun invoke(storeId: StoreId) = r.sync(storeId) }
class GetConflictsUseCase(private val r: SyncRepository) { suspend operator fun invoke(storeId: StoreId) = r.getConflicts(storeId) }
class ResolveConflictUseCase(private val r: SyncRepository) { suspend operator fun invoke(storeId: StoreId, id: EntityId, resolution: ConflictResolution) = r.resolveConflict(storeId, id, resolution) }
class AddOrderNoteUseCase(private val r: OrderNoteRepository) : AddOrderNote { override suspend fun invoke(storeId: StoreId, orderId: EntityId, content: String, customerNote: Boolean) = r.addNote(storeId, orderId, content, customerNote) }
class GetVariationsUseCase(private val r: VariationRepository) : GetVariations { override suspend fun invoke(storeId: StoreId, productId: EntityId, page: Int, perPage: Int) = r.list(storeId, productId, page, perPage) }
class GetVariationUseCase(private val r: VariationRepository) : GetVariation { override suspend fun invoke(storeId: StoreId, productId: EntityId, id: EntityId) = r.get(storeId, productId, id) }
class CreateVariationUseCase(private val r: VariationRepository) : CreateVariation { override suspend fun invoke(storeId: StoreId, value: Variation) = r.create(storeId, value) }
class UpdateVariationUseCase(private val r: VariationRepository) : UpdateVariation { override suspend fun invoke(storeId: StoreId, productId: EntityId, id: EntityId, value: Variation) = r.update(storeId, productId, id, value) }
class DeleteVariationUseCase(private val r: VariationRepository) : DeleteVariation { override suspend fun invoke(storeId: StoreId, productId: EntityId, id: EntityId) = r.delete(storeId, productId, id) }
class GetAttributesUseCase(private val r: AttributeRepository) : GetAttributes { override suspend fun invoke(storeId: StoreId, page: Int, perPage: Int) = r.list(storeId, page, perPage) }
class GetAttributeUseCase(private val r: AttributeRepository) : GetAttribute { override suspend fun invoke(storeId: StoreId, id: EntityId) = r.get(storeId, id) }
class CreateAttributeUseCase(private val r: AttributeRepository) : CreateAttribute { override suspend fun invoke(storeId: StoreId, value: GlobalAttribute) = r.create(storeId, value) }
class UpdateAttributeUseCase(private val r: AttributeRepository) : UpdateAttribute { override suspend fun invoke(storeId: StoreId, id: EntityId, value: GlobalAttribute) = r.update(storeId, id, value) }
class DeleteAttributeUseCase(private val r: AttributeRepository) : DeleteAttribute { override suspend fun invoke(storeId: StoreId, id: EntityId) = r.delete(storeId, id) }
class GetTermsUseCase(private val r: TermRepository) : GetTerms { override suspend fun invoke(storeId: StoreId, attributeId: EntityId, page: Int, perPage: Int) = r.list(storeId, attributeId, page, perPage) }
class GetTermUseCase(private val r: TermRepository) : GetTerm { override suspend fun invoke(storeId: StoreId, attributeId: EntityId, id: EntityId) = r.get(storeId, attributeId, id) }
class CreateTermUseCase(private val r: TermRepository) : CreateTerm { override suspend fun invoke(storeId: StoreId, attributeId: EntityId, value: AttributeTerm) = r.create(storeId, attributeId, value) }
class UpdateTermUseCase(private val r: TermRepository) : UpdateTerm { override suspend fun invoke(storeId: StoreId, attributeId: EntityId, id: EntityId, value: AttributeTerm) = r.update(storeId, attributeId, id, value) }
class DeleteTermUseCase(private val r: TermRepository) : DeleteTerm { override suspend fun invoke(storeId: StoreId, attributeId: EntityId, id: EntityId) = r.delete(storeId, attributeId, id) }
