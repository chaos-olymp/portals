package de.chaosolymp.portals.annotation_processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import de.chaosolymp.portals.annotations.messages.PluginMessage
import java.io.DataOutput
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.tools.Diagnostic

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_16) // to support Java 16
@SupportedAnnotationTypes("de.chaosolymp.portals.annotations.messages.PluginMessage")
@SupportedOptions(PluginMessageProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class PluginMessageProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(PluginMessage::class.java).forEach { element ->
            // Validate annotation target
            if (element.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be only applied to classes, but applied on Method $element")
                return false
            }

            // Check prerequisites
            val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
            if(generatedSourcesRoot.isEmpty()) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
                return false
            }

            val annotation = element.getAnnotation(PluginMessage::class.java)

            // Generate serialize method
            val result = generateSerialize(annotation, element, roundEnv)
            if(!result) return false

            // Generate deserialize method in companion object
            // TODO
        }

        return false
    }

    private fun generateSerialize(annotation: PluginMessage, element: Element, roundEnv: RoundEnvironment): Boolean {
        val funcBuilder = FunSpec.builder("serialize")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("output", ClassName("java.io", "DataOutput"))


        funcBuilder.addStatement("%L.serialize(output, %S)",
            ClassName("", ""),
            annotation.indicator
        )

        // Iterate through element tree
        element.enclosedElements.forEach { subElement ->
            if(subElement.kind == ElementKind.FIELD) {
                val fieldType = subElement.asType() as DeclaredType
                val fieldName = subElement.simpleName.toString()

                if(fieldType.kind == TypeKind.BOOLEAN) {
                    funcBuilder.addStatement("output.writeBoolean(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.BYTE) {
                    funcBuilder.addStatement("output.writeByte(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.SHORT) {
                    funcBuilder.addStatement("output.writeShort(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.CHAR) {
                    funcBuilder.addStatement("output.writeChar(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.INT) {
                    funcBuilder.addStatement("output.writeInt(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.LONG) {
                    funcBuilder.addStatement("output.writeLong(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.FLOAT) {
                    funcBuilder.addStatement("output.writeFloat(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.DOUBLE) {
                    funcBuilder.addStatement("output.writeDouble(%L)",
                        fieldName
                    )
                } else if(fieldType.kind == TypeKind.DECLARED) {
                    val className = Class.forName(fieldType.toString()).kotlin
                    if(className.qualifiedName == "kotlin.String") {
                        funcBuilder.addStatement("output.writeUTF(%L)",
                            fieldName
                        )
                    } else {
                        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Cannot process type ${className.qualifiedName}")
                        return false
                    }
                }
/*
                funcBuilder.addStatement("%L.serialize(output, %L)",
                    ClassName("de.chaosolymp.portals.core.messages", "MessageSerializer"),
                    fieldName
                )*/
            }
        }
        return true
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}