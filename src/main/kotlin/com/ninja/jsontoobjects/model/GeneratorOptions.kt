package com.ninja.jsontoobjects.model

enum class StructureMode {
    INNER_CLASS,
    SEPARATE_CLASSES,
    MULTIPLE_FILES
}

enum class TargetLanguage {
    JAVA,
    KOTLIN
}

data class JavaOptions(
    val useJsonProperty: Boolean = false,
    // Lombok annotations
    val useData: Boolean = true,
    val useGetter: Boolean = false,
    val useSetter: Boolean = false,
    val useNoArgsConstructor: Boolean = true,
    val useAllArgsConstructor: Boolean = true,
    // Manual generation (활성화 조건: 해당 Lombok 어노테이션 미선택시)
    val generateGetter: Boolean = true,
    val generateSetter: Boolean = true,
    val generateNoArgsConstructor: Boolean = true,
    val generateAllArgsConstructor: Boolean = false,
    // Structure
    val structureMode: StructureMode = StructureMode.INNER_CLASS,
    val useRecord: Boolean = false
) {
    // Lombok 사용 여부 (하나라도 선택되면 true)
    val useLombok: Boolean
        get() = useData || useGetter || useSetter || useNoArgsConstructor || useAllArgsConstructor

    // 실제로 Getter 생성할지 (Lombok으로 또는 수동으로)
    val shouldGenerateGetter: Boolean
        get() = !useData && !useGetter && generateGetter

    val shouldGenerateSetter: Boolean
        get() = !useData && !useSetter && generateSetter

    val shouldGenerateNoArgsConstructor: Boolean
        get() = !useNoArgsConstructor && generateNoArgsConstructor

    val shouldGenerateAllArgsConstructor: Boolean
        get() = !useAllArgsConstructor && generateAllArgsConstructor
}

data class KotlinOptions(
    val useJsonProperty: Boolean = false,
    val structureMode: StructureMode = StructureMode.SEPARATE_CLASSES
)

data class GeneratorOptions(
    val className: String,
    val packageName: String? = null,
    val targetLanguage: TargetLanguage = TargetLanguage.JAVA,
    val javaOptions: JavaOptions = JavaOptions(),
    val kotlinOptions: KotlinOptions = KotlinOptions()
)
