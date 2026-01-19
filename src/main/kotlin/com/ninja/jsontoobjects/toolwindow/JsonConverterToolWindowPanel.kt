package com.ninja.jsontoobjects.toolwindow

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.ninja.jsontoobjects.generator.JavaGenerator
import com.ninja.jsontoobjects.generator.KotlinGenerator
import com.ninja.jsontoobjects.model.*
import com.ninja.jsontoobjects.util.JavaVersionUtil
import com.ninja.jsontoobjects.util.JsonInputCleaner
import com.ninja.jsontoobjects.util.KotlinSupportUtil
import com.ninja.jsontoobjects.util.LombokUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class JsonConverterToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val classNameField = JBTextField("Generated")
    private val packageNameField = JBTextField()
    private val jsonInputArea = JTextArea(8, 40).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val formatButton = JButton("Format")
    private val generateButton = JButton("Generate")

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

    // Lombok annotations
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
        javaOptionsPanel = createJavaOptionsPanel()
        kotlinOptionsPanel = createKotlinOptionsPanel()
        setupListeners()
        updateRecordAvailability()
        updateKotlinAvailability()
        updateLombokAvailability()
        updateAllOptionsState()
        buildUI()
    }

    private fun buildUI() {
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.border = JBUI.Borders.empty(8)

        // Class name & Package name
        val classNamePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Package:"), packageNameField)
            .addLabeledComponent(JBLabel("Class Name:"), classNameField)
            .panel
        mainPanel.add(classNamePanel)

        // JSON input
        val jsonPanel = JPanel(BorderLayout())
        jsonPanel.border = BorderFactory.createTitledBorder("JSON Input")
        jsonPanel.add(JBScrollPane(jsonInputArea), BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(formatButton)
        jsonPanel.add(buttonPanel, BorderLayout.SOUTH)

        jsonPanel.preferredSize = Dimension(400, 180)
        jsonPanel.minimumSize = Dimension(200, 100)
        mainPanel.add(jsonPanel)

        // Language selection
        val languagePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        languagePanel.add(JBLabel("Target Language: "))
        languagePanel.add(javaRadio)
        languagePanel.add(kotlinRadio)
        mainPanel.add(languagePanel)

        // Options panels
        val optionsPanel = JPanel()
        optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
        optionsPanel.add(javaOptionsPanel)
        optionsPanel.add(kotlinOptionsPanel)
        kotlinOptionsPanel.isVisible = false
        mainPanel.add(optionsPanel)

        // Generate button
        val generatePanel = JPanel(FlowLayout(FlowLayout.CENTER))
        generateButton.preferredSize = Dimension(150, 30)
        generatePanel.add(generateButton)
        mainPanel.add(Box.createVerticalStrut(10))
        mainPanel.add(generatePanel)

        val scrollPane = JBScrollPane(mainPanel)
        scrollPane.border = null
        add(scrollPane, BorderLayout.CENTER)
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
        manualPanel.border = BorderFactory.createTitledBorder("Manual Generation")
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
        javaRadio.addActionListener { updateOptionsVisibility() }
        kotlinRadio.addActionListener { updateOptionsVisibility() }

        useRecordCheckbox.addActionListener { updateAllOptionsState() }
        useDataCheckbox.addActionListener { updateAllOptionsState() }

        useGetterCheckbox.addActionListener { updateManualOptionsEnabled() }
        useSetterCheckbox.addActionListener { updateManualOptionsEnabled() }
        useNoArgsConstructorCheckbox.addActionListener { updateManualOptionsEnabled() }
        useAllArgsConstructorCheckbox.addActionListener { updateManualOptionsEnabled() }

        formatButton.addActionListener { formatJsonInput() }
        generateButton.addActionListener { doGenerate() }
    }

    private fun formatJsonInput() {
        val formatted = prettyFormat(jsonInputArea.text)
        jsonInputArea.text = formatted
    }

    private fun updateOptionsVisibility() {
        javaOptionsPanel.isVisible = javaRadio.isSelected
        kotlinOptionsPanel.isVisible = kotlinRadio.isSelected
    }

    private fun updateRecordAvailability() {
        val languageLevel = LanguageLevelProjectExtension.getInstance(project)?.languageLevel
        val supportsRecord = JavaVersionUtil.supportsRecord(languageLevel)

        useRecordCheckbox.isEnabled = supportsRecord
        if (!supportsRecord) {
            useRecordCheckbox.isSelected = false
            val currentLevel = languageLevel?.presentableText ?: "Unknown"
            useRecordCheckbox.toolTipText = "Record requires Java 14+. Current project level: $currentLevel"
        }
    }

    private fun updateKotlinAvailability() {
        val modules = ModuleManager.getInstance(project).modules
        val anyModuleHasKotlin = modules.any { KotlinSupportUtil.isKotlinConfigured(it) }

        kotlinRadio.isEnabled = anyModuleHasKotlin
        if (!anyModuleHasKotlin) {
            kotlinRadio.toolTipText = "Kotlin is not configured for any module"
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

    private fun updateAllOptionsState() {
        val recordSelected = useRecordCheckbox.isSelected
        val dataSelected = useDataCheckbox.isSelected

        useDataCheckbox.isEnabled = !recordSelected
        useNoArgsConstructorCheckbox.isEnabled = !recordSelected
        useAllArgsConstructorCheckbox.isEnabled = !recordSelected

        val getterSetterEnabled = !recordSelected && !dataSelected
        useGetterCheckbox.isEnabled = getterSetterEnabled
        useSetterCheckbox.isEnabled = getterSetterEnabled

        updateManualOptionsEnabled()
    }

    private fun updateManualOptionsEnabled() {
        val recordSelected = useRecordCheckbox.isSelected

        if (recordSelected) {
            generateGetterCheckbox.isEnabled = false
            generateSetterCheckbox.isEnabled = false
            generateNoArgsConstructorCheckbox.isEnabled = false
            generateAllArgsConstructorCheckbox.isEnabled = false
            return
        }

        val getterByLombok = useDataCheckbox.isSelected || useGetterCheckbox.isSelected
        generateGetterCheckbox.isEnabled = !getterByLombok

        val setterByLombok = useDataCheckbox.isSelected || useSetterCheckbox.isSelected
        generateSetterCheckbox.isEnabled = !setterByLombok

        generateNoArgsConstructorCheckbox.isEnabled = !useNoArgsConstructorCheckbox.isSelected
        generateAllArgsConstructorCheckbox.isEnabled = !useAllArgsConstructorCheckbox.isSelected
    }

    private fun doGenerate() {
        // Validate class name
        val className = classNameField.text.trim()
        if (className.isBlank()) {
            Messages.showErrorDialog(project, "Class name is required", "Validation Error")
            return
        }
        if (!className.matches(Regex("^[A-Za-z_][A-Za-z0-9_]*$"))) {
            Messages.showErrorDialog(project, "Invalid class name", "Validation Error")
            return
        }

        // Validate JSON
        val json = JsonInputCleaner.clean(jsonInputArea.text)
        if (json.isBlank()) {
            Messages.showErrorDialog(project, "JSON input is required", "Validation Error")
            return
        }

        try {
            val parsed = JsonParser.parseString(json)
            if (!parsed.isJsonObject && !parsed.isJsonArray) {
                Messages.showErrorDialog(project, "JSON must be an object or array", "Validation Error")
                return
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
            Messages.showErrorDialog(project, message, "JSON Error")
            return
        }

        val options = getOptions()

        // Parse JSON
        val parser = com.ninja.jsontoobjects.parser.JsonParser()
        val parseResult = try {
            parser.parse(json, options.className)
        } catch (ex: Exception) {
            Messages.showErrorDialog(project, "Failed to parse JSON: ${ex.message}", "Parse Error")
            return
        }

        // Generate code
        val generatedFiles = when (options.targetLanguage) {
            TargetLanguage.JAVA -> {
                val generator = JavaGenerator(options.javaOptions)
                generator.generate(parseResult, options.className, options.packageName)
            }
            TargetLanguage.KOTLIN -> {
                val generator = KotlinGenerator(options.kotlinOptions)
                generator.generate(parseResult, options.className, options.packageName)
            }
        }

        // Get output directory
        val outputDir = project.guessProjectDir()
        if (outputDir == null) {
            // Copy to clipboard
            val content = generatedFiles.values.joinToString("\n\n")
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(java.awt.datatransfer.StringSelection(content), null)
            Messages.showInfoMessage(project, "Generated code copied to clipboard!", "Success")
            return
        }

        // Create files
        WriteCommandAction.runWriteCommandAction(project) {
            for ((fileName, content) in generatedFiles) {
                createOrUpdateFile(outputDir, fileName, content)
            }
        }

        // Open the first generated file
        generatedFiles.keys.firstOrNull()?.let { fileName ->
            outputDir.findChild(fileName)?.let { createdFile ->
                FileEditorManager.getInstance(project).openFile(createdFile, true)
            }
        }

        val fileCount = generatedFiles.size
        val message = if (fileCount == 1) {
            "Generated ${generatedFiles.keys.first()}"
        } else {
            "Generated $fileCount files"
        }
        Messages.showInfoMessage(project, message, "Success")
    }

    private fun createOrUpdateFile(parentDir: com.intellij.openapi.vfs.VirtualFile, fileName: String, content: String) {
        val existingFile = parentDir.findChild(fileName)
        if (existingFile != null) {
            existingFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
        } else {
            val newFile = parentDir.createChildData(this, fileName)
            newFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
        }
    }

    private fun getOptions(): GeneratorOptions {
        val targetLanguage = if (javaRadio.isSelected) TargetLanguage.JAVA else TargetLanguage.KOTLIN

        val javaStructureMode = when {
            innerClassRadio.isSelected -> StructureMode.INNER_CLASS
            separateClassesRadio.isSelected -> StructureMode.SEPARATE_CLASSES
            else -> StructureMode.MULTIPLE_FILES
        }

        val kotlinStructureMode = if (kotlinMultipleFilesCheckbox.isSelected)
            StructureMode.MULTIPLE_FILES else StructureMode.SEPARATE_CLASSES

        val packageName = packageNameField.text.trim().takeIf { it.isNotEmpty() }

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
                input
            }
        }
    }
}
