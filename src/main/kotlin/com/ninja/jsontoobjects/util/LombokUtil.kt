package com.ninja.jsontoobjects.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

object LombokUtil {

    /**
     * 프로젝트에서 Lombok이 사용 가능한지 확인
     * lombok.Data 클래스의 존재 여부로 판단
     */
    fun isLombokAvailable(project: Project?): Boolean {
        if (project == null) return false
        val scope = GlobalSearchScope.allScope(project)
        return JavaPsiFacade.getInstance(project)
            .findClass("lombok.Data", scope) != null
    }
}
