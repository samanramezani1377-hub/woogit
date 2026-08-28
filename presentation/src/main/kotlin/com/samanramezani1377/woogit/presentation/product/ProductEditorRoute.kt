package com.samanramezani1377.woogit.presentation.product

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.model.Attribute
import com.samanramezani1377.woogit.core.domain.model.IdName
import com.samanramezani1377.woogit.core.domain.model.Pricing
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductImage
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
import com.samanramezani1377.woogit.core.domain.model.Stock
import com.samanramezani1377.woogit.core.domain.model.StockStatus
import com.samanramezani1377.woogit.presentation.FeatureUiState
import com.samanramezani1377.woogit.presentation.V1PresentationDependencies
import com.samanramezani1377.woogit.presentation.vmFactory

@Composable
internal fun ProductEditorRoute(dependencies:V1PresentationDependencies,storeId:StoreId,productId:String?,onBack:()->Unit,onSaved:()->Unit,modifier:Modifier=Modifier){
    val vm=viewModel<ProductDetailViewModel>(factory=vmFactory{ProductDetailViewModel(dependencies)})
    val state by vm.state.collectAsState()
    var form by remember(productId){mutableStateOf<ProductEditorUiState.Editing?>(null)}
    LaunchedEffect(storeId,productId){if(productId!=null)vm.load(storeId,EntityId(productId))else form=ProductEditorUiState.Editing(null,"","","","","","","",null,"","")}
    LaunchedEffect(state){val product=(state as? FeatureUiState.Success)?.value ?: return@LaunchedEffect;form=ProductEditorUiState.Editing(product.id.value,product.name,product.sku.orEmpty(),product.shortDescription.orEmpty(),product.description.orEmpty(),product.pricing.regular.orEmpty(),product.pricing.sale.orEmpty(),product.stock?.quantity?.toString().orEmpty(),product.images.firstOrNull()?.src,product.categories.joinToString(", "){it.name},product.attributes.joinToString(" | "){"${it.name}:${it.options.joinToString(",")}"})}
    val editing=form
    when{
        editing!=null->ProductEditorScreen(editing,{form=editing.copy(name=it)},{form=editing.copy(sku=it)},{form=editing.copy(shortDescription=it)},{form=editing.copy(description=it)},{form=editing.copy(price=it)},{form=editing.copy(salePrice=it)},{form=editing.copy(stock=it)},{form=editing.copy(imageUrl=it.ifBlank{null})},{form=editing.copy(categories=it)},{form=editing.copy(attributes=it)},{val p=editing.toProduct();vm.save(storeId,p,editing.productId==null,onSaved);form=editing.copy(saving=true)},{if(productId!=null)vm.load(storeId,EntityId(productId))},onBack,modifier)
        state is FeatureUiState.Loading->ProductEditorScreen(ProductEditorUiState.Loading,{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},onBack=onBack,modifier=modifier)
        state is FeatureUiState.Error->ProductEditorScreen(ProductEditorUiState.Error(state.message,state.retryable),{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},onBack=onBack,onRetry={if(productId!=null)vm.load(storeId,EntityId(productId))},modifier=modifier)
    }
}

private fun ProductEditorUiState.Editing.toProduct():Product{
    val image=imageUrl?.takeIf{it.isNotBlank()}?.let{ProductImage(null,it,null,name)}
    val cats=categories.split(',').map{it.trim()}.filter{it.isNotBlank()}.mapIndexed{index,value->IdName(EntityId("category-$index"),value)}
    val attrs=attributes.split('|').mapNotNull{raw->val parts=raw.split(':',limit=2);if(parts.size!=2||parts[0].isBlank())null else Attribute(null,parts[0].trim(),true,true,parts[1].split(',').map{it.trim()}.filter{it.isNotBlank()})}
    return Product(EntityId(productId? : "new"),name.trim(),sku.trim().ifBlank{null},description.ifBlank{null},shortDescription.ifBlank{null},ProductStatus.DRAFT,ProductType.SIMPLE,Pricing(price.trim().ifBlank{null},salePrice.trim().ifBlank{null},salePrice.isNotBlank()),stock.toDoubleOrNull()?.let{Stock(it,if(it>0)StockStatus.IN_STOCK else StockStatus.OUT_OF_STOCK,true)},listOfNotNull(image),cats,attrs,null)
}
