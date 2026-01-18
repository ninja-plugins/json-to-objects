package com.ninja.jsontoobjects.util

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.facet.KotlinFacet

object KotlinSupportUtil {

    /**
     * 모듈에 Kotlin이 설정되어 있는지 확인
     * KotlinFacet 존재 여부로 판단
     */
    fun isKotlinConfigured(module: Module?): Boolean {
        if (module == null) return false
        return KotlinFacet.get(module) != null
    }
}
