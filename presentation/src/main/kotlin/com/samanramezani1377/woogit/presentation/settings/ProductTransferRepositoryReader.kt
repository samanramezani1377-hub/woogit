package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class ProductTransferRepositoryReader(private val d:V1PresentationDependencies){
    suspend fun products(s:StoreId,p:(ProductTransferProgress)->Unit):List<Product>{val out=mutableListOf<Product>();var page=1;while(true){val b=when(val r=d.getProducts(s,page,100,null)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت محصولات ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;p(ProductTransferProgress("در حال دریافت محصولات…",out.size,out.size));if(b.size<100)break;page++};return out.distinctBy{it.id.value}}
    suspend fun variations(s:StoreId,id:EntityId):List<Variation>{val out=mutableListOf<Variation>();var page=1;while(true){val b=when(val r=d.getVariations(s,id,page,100)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت Variationهای محصول ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<100)break;page++};return out.distinctBy{it.id.value}}
    suspend fun categories(s:StoreId):List<IdName>{val out=mutableListOf<IdName>();var page=1;while(true){val b=when(val r=d.getProductCategories(s,page,100,null)){is CoreResult.Success->r.value;is CoreResult.Failure->break};if(b.isEmpty())break;out+=b;if(b.size<100)break;page++};return out.distinctBy{it.id.value}}
    suspend fun media(s:StoreId):List<ProductImage>{val out=mutableListOf<ProductImage>();var page=1;while(true){val b=when(val r=d.getMedia(s,page,100,null)){is CoreResult.Success->r.value;is CoreResult.Failure->break};if(b.isEmpty())break;out+=b;if(b.size<100)break;page++};return out.distinctBy{it.id.value}}
}