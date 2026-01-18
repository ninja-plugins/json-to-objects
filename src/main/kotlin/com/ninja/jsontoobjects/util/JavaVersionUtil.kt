package com.ninja.jsontoobjects.util

import com.intellij.pom.java.LanguageLevel

object JavaVersionUtil {

    /**
     * Record는 Java 14에서 preview로 도입, Java 16에서 정식 기능이 됨
     * 여기서는 Java 14 이상이면 Record 지원으로 판단
     */
    fun supportsRecord(languageLevel: LanguageLevel?): Boolean {
        return languageLevel != null && languageLevel >= LanguageLevel.JDK_14
    }
}
