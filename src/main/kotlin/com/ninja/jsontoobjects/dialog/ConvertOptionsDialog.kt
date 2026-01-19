package com.ninja.jsontoobjects.dialog

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.ninja.jsontoobjects.model.*
import com.ninja.jsontoobjects.util.JavaVersionUtil
import com.ninja.jsontoobjects.util.JsonInputCleaner
import com.ninja.jsontoobjects.util.KotlinSupportUtil
import com.ninja.jsontoobjects.util.LombokUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class ConvertOptionsDialog(
    private val project: Project?,
    private val module: Module?,
    private val suggestedClassName: String,
    private val initialJson: String = "",
    private val suggestedPackage: String? = null
) : DialogWrapper(project) {

    private val classNameField = JBTextField(suggestedClassName)
    private val packageNameField: JComponent = if (project != null) {
        TextFieldWithAutoCompletion.create(
            project,
            collectPackageNames(project),
            true,
            suggestedPackage ?: ""
        )
    } else {
        JBTextField(suggestedPackage ?: "")
    }

    private fun getPackageName(): String {
        return when (val field = packageNameField) {
            is TextFieldWithAutoCompletion<*> -> field.text
            is JBTextField -> field.text
            else -> ""
        }
    }
    private val jsonInputArea = JTextArea(prettyFormat(initialJson), 8, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val formatButton = JButton("Format")

    // Language selection
    private val javaRadio = JRadioButton("Java", true)
    private val kotlinRadio = JRadioButton("Kotlin")
    private val languageGroup = ButtonGroup().apply {
        add(javaRadio)
        add(kotlinRadio)
    }

    // Java options
    private val useJsonPropertyCheckbox = JCheckBox("Use @JsonProperty")
    private val useRecordCheckbox = JCheckBox("Use Java Record (14+)")

    // Lombok annotations (각각 체크박스)
    private val useDataCheckbox = JCheckBox("@Data", true)
    private val useGetterCheckbox = JCheckBox("@Getter")
    private val useSetterCheckbox = JCheckBox("@Setter")
    private val useNoArgsConstructorCheckbox = JCheckBox("@NoArgsConstructor", true)
    private val useAllArgsConstructorCheckbox = JCheckBox("@AllArgsConstructor", true)

    // Manual generation options
    private val generateGetterCheckbox = JCheckBox("Generate Getter", true)
    private val generateSetterCheckbox = JCheckBox("Generate Setter", true)
    private val generateNoArgsConstructorCheckbox = JCheckBox("Generate NoArgs Constructor", true)
    private val generateAllArgsConstructorCheckbox = JCheckBox("Generate AllArgs Constructor")

    // Structure options
    private val innerClassRadio = JRadioButton("Inner Class", true)
    private val separateClassesRadio = JRadioButton("Separate Classes (same file)")
    private val multipleFilesRadio = JRadioButton("Multiple Files")
    private val structureGroup = ButtonGroup().apply {
        add(innerClassRadio)
        add(separateClassesRadio)
        add(multipleFilesRadio)
    }

    // Kotlin options
    private val kotlinJsonPropertyCheckbox = JCheckBox("Use @JsonProperty")
    private val kotlinMultipleFilesCheckbox = JCheckBox("Multiple Files")

    private val javaOptionsPanel: JPanel
    private val kotlinOptionsPanel: JPanel

    init {
        title = "JSON to Java/Kotlin"
        javaOptionsPanel = createJavaOptionsPanel()
        kotlinOptionsPanel = createKotlinOptionsPanel()
        setupListeners()
        updateRecordAvailability()
        updateKotlinAvailability()
        updateLombokAvailability()
        updateAllOptionsState()
        init()
    }

    private fun updateRecordAvailability() {
        val languageLevel = project?.let {
            LanguageLevelProjectExtension.getInstance(it)?.languageLevel
        }

        val supportsRecord = JavaVersionUtil.supportsRecord(languageLevel)

        useRecordCheckbox.isEnabled = supportsRecord
        if (!supportsRecord) {
            useRecordCheckbox.isSelected = false
            val currentLevel = languageLevel?.presentableText ?: "Unknown"
            useRecordCheckbox.toolTipText = "Record requires Java 14+. Current project level: $currentLevel"
        }
    }

    private fun updateKotlinAvailability() {
        val isKotlinConfigured = KotlinSupportUtil.isKotlinConfigured(module)

        kotlinRadio.isEnabled = isKotlinConfigured
        if (!isKotlinConfigured) {
            kotlinRadio.toolTipText = "Kotlin is not configured for this module"
            // Java가 선택되어 있는지 확인하고, 아니면 Java로 전환
            if (kotlinRadio.isSelected) {
                javaRadio.isSelected = true
                updateOptionsVisibility()
            }
        }
    }

    private fun updateLombokAvailability() {
        val isLombokAvailable = LombokUtil.isLombokAvailable(project)

        val lombokCheckboxes = listOf(
            useDataCheckbox,
            useGetterCheckbox,
            useSetterCheckbox,
            useNoArgsConstructorCheckbox,
            useAllArgsConstructorCheckbox
        )

        if (!isLombokAvailable) {
            val tooltipMessage = "Lombok is not available in this project"
            lombokCheckboxes.forEach { checkbox ->
                checkbox.isEnabled = false
                checkbox.isSelected = false
                checkbox.toolTipText = tooltipMessage
            }
        }
    }

    private fun createJavaOptionsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Java Options")

        panel.add(useJsonPropertyCheckbox)
        panel.add(useRecordCheckbox)

        panel.add(Box.createVerticalStrut(10))

        // Lombok annotations section
        val lombokPanel = JPanel()
        lombokPanel.layout = BoxLayout(lombokPanel, BoxLayout.Y_AXIS)
        lombokPanel.border = BorderFactory.createTitledBorder("Lombok Annotations")
        lombokPanel.add(useDataCheckbox)
        lombokPanel.add(useGetterCheckbox)
        lombokPanel.add(useSetterCheckbox)
        lombokPanel.add(useNoArgsConstructorCheckbox)
        lombokPanel.add(useAllArgsConstructorCheckbox)
        panel.add(lombokPanel)

        panel.add(Box.createVerticalStrut(5))

        // Manual generation section
        val manualPanel = JPanel()
        manualPanel.layout = BoxLayout(manualPanel, BoxLayout.Y_AXIS)
        manualPanel.border = BorderFactory.createTitledBorder("Manual Generation (Lombok 미선택시)")
        manualPanel.add(generateGetterCheckbox)
        manualPanel.add(generateSetterCheckbox)
        manualPanel.add(generateNoArgsConstructorCheckbox)
        manualPanel.add(generateAllArgsConstructorCheckbox)
        panel.add(manualPanel)

        panel.add(Box.createVerticalStrut(5))

        // Structure section
        val structurePanel = JPanel()
        structurePanel.layout = BoxLayout(structurePanel, BoxLayout.Y_AXIS)
        structurePanel.border = BorderFactory.createTitledBorder("Structure")
        structurePanel.add(innerClassRadio)
        structurePanel.add(separateClassesRadio)
        structurePanel.add(multipleFilesRadio)
        panel.add(structurePanel)

        return panel
    }

    private fun createKotlinOptionsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createTitledBorder("Kotlin Options")

        panel.add(kotlinJsonPropertyCheckbox)
        panel.add(kotlinMultipleFilesCheckbox)

        return panel
    }

    private fun setupListeners() {
        // Language change
        javaRadio.addActionListener { updateOptionsVisibility() }
        kotlinRadio.addActionListener { updateOptionsVisibility() }

        // Record 선택시 Lombok & Manual 비활성화
        useRecordCheckbox.addActionListener { updateAllOptionsState() }

        // @Data 선택시 @Getter/@Setter 비활성화
        useDataCheckbox.addActionListener { updateAllOptionsState() }

        // Lombok 체크박스 변경시 Manual 옵션 활성화/비활성화
        useGetterCheckbox.addActionListener { updateManualOptionsEnabled() }
        useSetterCheckbox.addActionListener { updateManualOptionsEnabled() }
        useNoArgsConstructorCheckbox.addActionListener { updateManualOptionsEnabled() }
        useAllArgsConstructorCheckbox.addActionListener { updateManualOptionsEnabled() }

        // Format 버튼
        formatButton.addActionListener { formatJsonInput() }
    }

    private fun formatJsonInput() {
        val formatted = prettyFormat(jsonInputArea.text)
        jsonInputArea.text = formatted
    }

    private fun updateOptionsVisibility() {
        javaOptionsPanel.isVisible = javaRadio.isSelected
        kotlinOptionsPanel.isVisible = kotlinRadio.isSelected
    }

    private fun updateAllOptionsState() {
        val recordSelected = useRecordCheckbox.isSelected
        val dataSelected = useDataCheckbox.isSelected

        // Record 선택시 Lombok 전체 비활성화
        useDataCheckbox.isEnabled = !recordSelected
        useNoArgsConstructorCheckbox.isEnabled = !recordSelected
        useAllArgsConstructorCheckbox.isEnabled = !recordSelected

        // Record 선택 또는 @Data 선택시 @Getter/@Setter 비활성화
        val getterSetterEnabled = !recordSelected && !dataSelected
        useGetterCheckbox.isEnabled = getterSetterEnabled
        useSetterCheckbox.isEnabled = getterSetterEnabled

        updateManualOptionsEnabled()
    }

    private fun updateManualOptionsEnabled() {
        val recordSelected = useRecordCheckbox.isSelected

        // Record 선택시 Manual Generation 전체 비활성화
        if (recordSelected) {
            generateGetterCheckbox.isEnabled = false
            generateSetterCheckbox.isEnabled = false
            generateNoArgsConstructorCheckbox.isEnabled = false
            generateAllArgsConstructorCheckbox.isEnabled = false
            return
        }

        // Getter: @Data 또는 @Getter 선택시 비활성화
        val getterByLombok = useDataCheckbox.isSelected || useGetterCheckbox.isSelected
        generateGetterCheckbox.isEnabled = !getterByLombok

        // Setter: @Data 또는 @Setter 선택시 비활성화
        val setterByLombok = useDataCheckbox.isSelected || useSetterCheckbox.isSelected
        generateSetterCheckbox.isEnabled = !setterByLombok

        // NoArgsConstructor: @NoArgsConstructor 선택시 비활성화
        generateNoArgsConstructorCheckbox.isEnabled = !useNoArgsConstructorCheckbox.isSelected

        // AllArgsConstructor: @AllArgsConstructor 선택시 비활성화
        generateAllArgsConstructorCheckbox.isEnabled = !useAllArgsConstructorCheckbox.isSelected
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())

        // Top section: Class name + JSON input
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)

        // Class name & Package name
        val classNamePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Package:"), packageNameField)
            .addLabeledComponent(JBLabel("Class Name:"), classNameField)
            .panel
        topPanel.add(classNamePanel)

        // JSON input
        val jsonPanel = JPanel(BorderLayout())
        jsonPanel.border = BorderFactory.createTitledBorder("JSON Input")
        jsonPanel.add(JScrollPane(jsonInputArea), BorderLayout.CENTER)

        // Format 버튼 (오른쪽 하단)
        val buttonPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.RIGHT))
        buttonPanel.add(formatButton)
        jsonPanel.add(buttonPanel, BorderLayout.SOUTH)

        jsonPanel.preferredSize = Dimension(400, 180)
        topPanel.add(jsonPanel)

        // Language selection
        val languagePanel = JPanel()
        languagePanel.add(JBLabel("Target Language: "))
        languagePanel.add(javaRadio)
        languagePanel.add(kotlinRadio)
        topPanel.add(languagePanel)

        mainPanel.add(topPanel, BorderLayout.NORTH)

        // Options panels (scrollable)
        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.add(javaOptionsPanel)
        optionsPanel.add(kotlinOptionsPanel)

        kotlinOptionsPanel.isVisible = false

        val scrollPane = JScrollPane(optionsPanel)
        scrollPane.border = null
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.preferredSize = Dimension(450, 650)

        return mainPanel
    }

    fun getJsonInput(): String = JsonInputCleaner.clean(jsonInputArea.text)

    override fun doValidate(): ValidationInfo? {
        val className = classNameField.text.trim()
        if (className.isBlank()) {
            return ValidationInfo("Class name is required", classNameField)
        }
        if (!className.matches(Regex("^[A-Za-z_][A-Za-z0-9_]*$"))) {
            return ValidationInfo("Invalid class name", classNameField)
        }

        val json = JsonInputCleaner.clean(jsonInputArea.text)
        if (json.isBlank()) {
            return ValidationInfo("JSON input is required", jsonInputArea)
        }

        try {
            val parsed = JsonParser.parseString(json)
            if (!parsed.isJsonObject && !parsed.isJsonArray) {
                return ValidationInfo("JSON must be an object or array", jsonInputArea)
            }
        } catch (e: JsonSyntaxException) {
            val message = e.message?.let {
                val lineMatch = Regex("line (\\d+) column (\\d+)").find(it)
                if (lineMatch != null) {
                    "Invalid JSON at line ${lineMatch.groupValues[1]}, column ${lineMatch.groupValues[2]}"
                } else {
                    "Invalid JSON syntax"
                }
            } ?: "Invalid JSON syntax"
            return ValidationInfo(message, jsonInputArea)
        }

        return null
    }

    fun getOptions(): GeneratorOptions {
        val targetLanguage = if (javaRadio.isSelected) TargetLanguage.JAVA else TargetLanguage.KOTLIN

        val javaStructureMode = when {
            innerClassRadio.isSelected -> StructureMode.INNER_CLASS
            separateClassesRadio.isSelected -> StructureMode.SEPARATE_CLASSES
            else -> StructureMode.MULTIPLE_FILES
        }

        val kotlinStructureMode = if (kotlinMultipleFilesCheckbox.isSelected)
            StructureMode.MULTIPLE_FILES else StructureMode.SEPARATE_CLASSES

        val packageName = getPackageName().trim().takeIf { it.isNotEmpty() }

        return GeneratorOptions(
            className = classNameField.text.trim(),
            packageName = packageName,
            targetLanguage = targetLanguage,
            javaOptions = JavaOptions(
                useJsonProperty = useJsonPropertyCheckbox.isSelected,
                useData = useDataCheckbox.isSelected,
                useGetter = useGetterCheckbox.isSelected,
                useSetter = useSetterCheckbox.isSelected,
                useNoArgsConstructor = useNoArgsConstructorCheckbox.isSelected,
                useAllArgsConstructor = useAllArgsConstructorCheckbox.isSelected,
                generateGetter = generateGetterCheckbox.isSelected,
                generateSetter = generateSetterCheckbox.isSelected,
                generateNoArgsConstructor = generateNoArgsConstructorCheckbox.isSelected,
                generateAllArgsConstructor = generateAllArgsConstructorCheckbox.isSelected,
                structureMode = javaStructureMode,
                useRecord = useRecordCheckbox.isSelected
            ),
            kotlinOptions = KotlinOptions(
                useJsonProperty = kotlinJsonPropertyCheckbox.isSelected,
                structureMode = kotlinStructureMode
            )
        )
    }

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()

        fun prettyFormat(input: String): String {
            if (input.isBlank()) return input
            return try {
                val cleaned = JsonInputCleaner.clean(input)
                val jsonElement = JsonParser.parseString(cleaned)
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                input // 파싱 실패시 원본 반환
            }
        }

        fun collectPackageNames(project: Project): Collection<String> {
            val packages = mutableSetOf<String>()

            ProjectRootManager.getInstance(project).contentSourceRoots.forEach { sourceRoot ->
                collectPackagesFromDirectory(sourceRoot, "", packages)
            }

            return packages.sorted()
        }

        private fun collectPackagesFromDirectory(
            dir: VirtualFile,
            currentPackage: String,
            packages: MutableSet<String>
        ) {
            if (!dir.isDirectory) return

            val hasSourceFiles = dir.children.any {
                it.extension == "java" || it.extension == "kt"
            }
            if (hasSourceFiles && currentPackage.isNotEmpty()) {
                packages.add(currentPackage)
            }

            dir.children.filter { it.isDirectory && !it.name.startsWith(".") }.forEach { subDir ->
                val subPackage = if (currentPackage.isEmpty()) subDir.name else "$currentPackage.${subDir.name}"
                collectPackagesFromDirectory(subDir, subPackage, packages)
            }
        }
    }
}
