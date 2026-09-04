package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class ProductImportCategoryResolver(
    private val d: V1PresentationDependencies
) {
    suspend fun resolve(
        storeId: StoreId,
        source: List<TransferCategory>,
        destination: List<IdName>,
        createMissing: Boolean
    ): CategoryMapping {
        val sourceById = source.associateBy { it.id }
        val resolved = mutableMapOf<String, IdName>()
        var created = 0
        var reused = 0
        val destinationByKey = destination
            .groupBy { normalize(it.name) + "|" + normalize(it.parentId?.value) }
            .toMutableMap()

        suspend fun resolveOne(id: String): IdName? {
            resolved[id]?.let { return it }
            val category = sourceById[id] ?: return null
            val parent = category.parentId?.let { resolveOne(it) }
            val key = normalize(category.name) + "|" + normalize(parent?.id?.value)
            val found = destinationByKey[key].orEmpty().firstOrNull()
            if (found != null) {
                resolved[id] = found
                reused++
                return found
            }
            if (!createMissing) return null
            return when (val result = d.getProductCategories.create(
                storeId,
                IdName(EntityId(NEW_ID_PLACEHOLDER), category.name, parent?.id)
            )) {
                is CoreResult.Success -> {
                    resolved[id] = result.value
                    destinationByKey[key] = destinationByKey[key].orEmpty() + result.value
                    created++
                    result.value
                }
                is CoreResult.Failure -> null
            }
        }

        source.forEach { resolveOne(it.id) }
        return CategoryMapping(resolved, created, reused)
    }
}

internal data class CategoryMapping(
    val items: Map<String, IdName>,
    val created: Int,
    val resolved: Int
)
