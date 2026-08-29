package com.samanramezani1377.woogit.presentation.settings

internal fun validateTransferProduct(x:TransferProduct):String?=when{ x.id.isBlank()->"محصول بدون شناسه است و وارد نشد.";x.name.isBlank()->"محصول با شناسه ${x.id} فاقد نام است و وارد نشد.";x.type.isBlank()->"محصول «${x.name}» فاقد نوع است و وارد نشد.";x.status.isBlank()->"محصول «${x.name}» فاقد وضعیت است و وارد نشد.";else->null }