package com.mrboomdev.awery.ext.data

import java.io.Serial
import java.io.Serializable

class CatalogTag @JvmOverloads constructor(
	val name: String,
	val description: String? = null,
	val isAdult: Boolean = false,
	val isSpoiler: Boolean = false
) : Serializable {
	companion object {
		@Serial
		private val serialVersionUID: Long = 1
	}
}