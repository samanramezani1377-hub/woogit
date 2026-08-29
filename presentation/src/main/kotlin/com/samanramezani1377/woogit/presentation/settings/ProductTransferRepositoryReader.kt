package com.samanramezani1377.woogit.presentation.settings

import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult
import com.samanramezani1377.woogit.core.domain.model.*
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies

internal class ProductTransferRepositoryReader(private val d:V1PresentationDependencies,private val pageSize:Int=100){
    suspend fun products(s:StoreId,p:(ProductTransferProgress)->Unit):List<Product>{val out=mutableListOf<Product>();var page=1;while(true){val b=when(val r=d.getProducts(s,page,pageSize,null)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت محصولات ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;p(ProductTransferProgress("در حال دریافت محصولات…",out.size,out.size));if(b.size<pageSize)break;page++};return out.distinctBy{it.id.value}}
    suspend fun variations(s:StoreId,id:EntityId):List<Variation>{val out=mutableListOf<Variation>();var page=1;while(true){val b=when(val r=d.getVariations(s,id,page,pageSize)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت Variationهای محصول ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<pageSize)break;page++};return out.distinctBy{it.id.value}}
    suspend fun categories(s:StoreId):List<IdName>{val out=mutableListOf<IdName>();var page=1;while(true){val b=when(val r=d.getProductCategories(s,page,pageSize,null)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت دسته‌بندی‌ها ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<pageSize)break;page++};return out.distinctBy{it.id.value}}
    suspend fun media(s:StoreId):List<ProductImage>{val out=mutableListOf<ProductImage>();var page=1;while(true){val b=when(val r=d.getMedia(s,page,pageSize,null)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت رسانه‌ها ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<pageSize)break;page++};return out.distinctBy{it.id?.value}}
    suspend fun attributes(s:StoreId):List<GlobalAttribute>{val out=mutableListOf<GlobalAttribute>();var page=1;while(true){val b=when(val r=d.getAttributes(s,page,pageSize)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت ویژگی‌های سراسری ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<pageSize)break;page++};return out.distinctBy{it.id.value}}
    suspend fun terms(s:StoreId,attributeId:EntityId):List<AttributeTerm>{val out=mutableListOf<AttributeTerm>();var page=1;while(true){val b=when(val r=d.getTerms(s,attributeId,page,pageSize)){is CoreResult.Success->r.value;is CoreResult.Failure->error("دریافت Termهای ویژگی ناموفق بود: ${r.error}")};if(b.isEmpty())break;out+=b;if(b.size<pageSize)break;page++};return out.distinctBy{it.id.value}}
}
