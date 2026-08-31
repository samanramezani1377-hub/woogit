package com.samanramezani1377.woogit.presentation.settings

enum class ProductImportMode {
    CREATE_NEW,
    CREATE_NEW_DRAFT,
    UPDATE_EXISTING,
}

data class ProductImportOptions(
    val allowUnexpectedPublish: Boolean = false,
    val addMissingCategories: Boolean = false,
    val addMissingAttributes: Boolean = false,
    val uploadAllImagesWithoutLibraryCheck: Boolean = false,
)