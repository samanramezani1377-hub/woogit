package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.AttributeTerm
import com.samanramezani1377.woogit.core.domain.model.GlobalAttribute
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class ProductImportAttributeResolver(
    private val d: V1PresentationDependencies,
    private val reader: ProductTransferRepositoryReader
) {
    suspend fun resolve(
        storeId: StoreId,
        source: List<TransferGlobalAttribute>,
        createMissing: Boolean,
        preserveIds: Boolean
    ): GlobalMapping {
        val destination = reader.attributes(storeId)
        val items = mutableMapOf<String, EntityId>()
        var created = 0
        var resolved = 0
        var termsCreated = 0
        var termsResolved = 0

        for (global in source) {
            val existing = if (preserveIds) {
                destination.firstOrNull { it.id.value == global.id }
            } else {
                destination.firstOrNull {
                    normalize(it.slug) == normalize(global.slug) ||
                        normalize(it.name) == normalize(global.name)
                }
            }
            if (existing == null && !createMissing) continue

            val attribute = existing ?: when (val result = d.createAttribute(
                storeId,
                GlobalAttribute(EntityId(NEW_ID_PLACEHOLDER), global.name, global.slug, emptyList())
            )) {
                is CoreResult.Success -> {
                    created++
                    result.value
                }
                is CoreResult.Failure -> continue
            }

            if (existing != null) resolved++
            items[global.id] = attribute.id

            val destinationTerms = reader.terms(storeId, attribute.id)
            for (term in global.terms) {
                val found = destinationTerms.firstOrNull {
                    normalize(it.name) == normalize(term.name) ||
                        normalize(it.slug) == normalize(term.slug)
                }
                if (found != null) {
                    termsResolved++
                    continue
                }
                if (!createMissing) continue
                when (d.createTerm(
                    storeId,
                    attribute.id,
                    AttributeTerm(EntityId(NEW_ID_PLACEHOLDER), term.name, term.slug)
                )) {
                    is CoreResult.Success -> termsCreated++
                    is CoreResult.Failure -> Unit
                }
            }
        }
        return GlobalMapping(items, created, resolved, termsCreated, termsResolved)
    }
}

internal data class GlobalMapping(
    val items: Map<String, EntityId>,
    val created: Int,
    val resolved: Int,
    val termsCreated: Int,
    val termsResolved: Int
)
