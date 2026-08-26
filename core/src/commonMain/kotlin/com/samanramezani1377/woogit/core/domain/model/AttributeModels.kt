package com.samanramezani1377.woogit.core.domain.model

import com.samanramezani1377.woogit.core.domain.entity.EntityId

data class GlobalAttribute(val id: EntityId, val name: String, val slug: String, val terms: List<AttributeTerm>)
data class CustomAttribute(val name: String, val visible: Boolean, val variation: Boolean, val options: List<String>)
data class AttributeTerm(val id: EntityId?, val name: String, val slug: String?)
data class Attribute(val id: EntityId?, val name: String, val visible: Boolean, val variation: Boolean, val options: List<String>)
