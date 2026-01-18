package com.ninja.jsontoobjects.util

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module

object KotlinSupportUtil {

    private const val KOTLIN_FACET_TYPE_ID = "kotlin-language"

    /**
     * 모듈에 Kotlin이 설정되어 있는지 확인
     * FacetManager를 통해 Kotlin facet 존재 여부로 판단 (K2 모드 호환)
     */
    fun isKotlinConfigured(module: Module?): Boolean {
        if (module == null) return false
        return try {
            val facetManager = FacetManager.getInstance(module)
            facetManager.allFacets.any {
                it.typeId.toString() == KOTLIN_FACET_TYPE_ID
            }
        } catch (e: Exception) {
            false
        }
    }
}
