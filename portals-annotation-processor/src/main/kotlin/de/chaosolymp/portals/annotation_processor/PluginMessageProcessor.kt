package de.chaosolymp.portals.annotation_processor

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import de.chaosolymp.portals.annotations.messages.PluginMessage
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.tools.Diagnostic

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_16) // to support Java 16
@SupportedAnnotationTypes("de.chaosolymp.portals.annotations.messages.PluginMessage")
@SupportedOptions(PluginMessageProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class PluginMessageProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        // Check prerequisites
        val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if(generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        val file = File(generatedSourcesRoot).apply { mkdir() }
        val fileBuilder = FileSpec.builder("de.chaosolymp.portals.core.messages.generated", "PluginMessageGenerated")

        fileBuilder.addComment(
            """
                CAUTION
                
                THIS FILE IS AUTO-GENERATED USING portals-annotation-processor.
                DO NOT EDIT!
            """.trimIndent()
        )

        val alreadyProcessedElements = mutableListOf<Element>()
        for(element in roundEnv.getElementsAnnotatedWith(PluginMessage::class.java)) {
            // Validate annotation target
            if (element.kind != ElementKind.CLASS) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can be only applied to classes, but applied on Method $element")
                return false
            }
            alreadyProcessedElements.add(element)

            val annotation = element.getAnnotation(PluginMessage::class.java)

            // Generate serialize method
            generateSpecificSerialize(annotation, element)?.let { funSpec ->
                fileBuilder.addFunction(funSpec)
            }

            // Generate deserialize method in companion object
            generateSpecificDeserialize(element)?.let { funSpec ->
                fileBuilder.addFunction(funSpec)
            }
        }

        if(alreadyProcessedElements.isEmpty()) return false

        // Generate serialize pattern method
        val serializeFuncBuilder = FunSpec
            .builder("serialize")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("message", ClassName("de.chaosolymp.portals.core.messages", "AbstractPluginMessage"))
            .addParameter("output", ClassName("java.io", "DataOutput"))

        for((i, element) in alreadyProcessedElements.withIndex()) {
            val messageClassName = element.asType().asTypeName().toString().substringAfterLast('.')

            val elementTypeName = element.asType().asTypeName()
            if(i == 0) serializeFuncBuilder.addStatement("if(message is %L) serialize$messageClassName(message, output)", elementTypeName)
            else serializeFuncBuilder.addStatement("else if(message is %L) serialize$messageClassName(message, output)", elementTypeName)

            if(i == alreadyProcessedElements.size - 1) serializeFuncBuilder.addStatement("else throw IllegalArgumentException()")
        }

        // Generate deserialize pattern method
        val deserializeFuncBuilder = FunSpec.builder("deserialize")
            .addModifiers(KModifier.PUBLIC)
            .addParameter("input", ClassName("java.io", "DataInput"))
            .returns(ClassName("de.chaosolymp.portals.core.messages", "AbstractPluginMessage").asNullable())

        deserializeFuncBuilder.addStatement("val identifier = input.readUTF()")
        for((i, element) in alreadyProcessedElements.withIndex()) {
            val annotation = element.getAnnotation(PluginMessage::class.java)
            val messageClassName = element.asType().asTypeName().toString().substringAfterLast('.')
            val annotationIdentifier = annotation.indicator

            if(i == 0) deserializeFuncBuilder.addStatement("if(identifier == %S) return deserialize$messageClassName(input)", annotationIdentifier)
            else deserializeFuncBuilder.addStatement("else if(identifier == %S) return deserialize$messageClassName(input)", annotationIdentifier)

            if(i == alreadyProcessedElements.size - 1) deserializeFuncBuilder.addStatement("else return null")
        }

        fileBuilder.addFunction(serializeFuncBuilder.build())
        fileBuilder.addFunction(deserializeFuncBuilder.build())

        fileBuilder.build().writeTo(file)
        return true
    }

    private fun generateSpecificSerialize(annotation: PluginMessage, element: Element): FunSpec? {
        val messageClassName = element.asType().asTypeName().toString().substringAfterLast('.')

        val funcBuilder = FunSpec.builder("serialize$messageClassName")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("input", element.asType().asTypeName())
            .addParameter("output", ClassName("java.io", "DataOutput"))

        funcBuilder.addStatement("output.writeUTF(%S)",
            annotation.indicator
        )

        // Iterate through element tree
        element.enclosedElements.forEach { subElement ->
            if(subElement.kind == ElementKind.FIELD) {
                val fieldType = subElement.asType()
                val fieldName = subElement.simpleName.toString()

                when (fieldType.kind) {
                    TypeKind.BOOLEAN -> {
                        funcBuilder.addStatement("output.writeBoolean(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.BYTE -> {
                        funcBuilder.addStatement("output.writeByte(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.SHORT -> {
                        funcBuilder.addStatement("output.writeShort(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.CHAR -> {
                        funcBuilder.addStatement("output.writeChar(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.INT -> {
                        funcBuilder.addStatement("output.writeInt(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.LONG -> {
                        funcBuilder.addStatement("output.writeLong(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.FLOAT -> {
                        funcBuilder.addStatement("output.writeFloat(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.DOUBLE -> {
                        funcBuilder.addStatement("output.writeDouble(input.%L)",
                            fieldName
                        )
                    }
                    TypeKind.DECLARED -> {
                        val className = Class.forName(fieldType.toString()).kotlin
                        when (className.qualifiedName) {
                            "kotlin.String" -> {
                                funcBuilder.addStatement("output.writeUTF(input.%L)",
                                    fieldName
                                )
                            }
                            "java.util.UUID" -> {
                                funcBuilder.addStatement("output.write(%L.getBytesFromUUID(input.%L))",
                                    ClassName("de.chaosolymp.portals.core", "UUIDUtils"), fieldName)
                            }
                            else -> {
                                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Cannot process type ${className.qualifiedName}")
                                return null
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        return funcBuilder.build()
    }

    private fun generateSpecificDeserialize(element: Element): FunSpec? {
        val messageClassName = element.asType().asTypeName().toString().substringAfterLast('.')

        val funcBuilder = FunSpec.builder("deserialize$messageClassName")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("input", ClassName("java.io", "DataInput"))
            .returns(element.asType().asTypeName())

        val constructorParameters = mutableListOf<String>()

        // Iterate through element tree
        element.enclosedElements.forEach { subElement ->
            if(subElement.kind == ElementKind.FIELD) {
                val fieldType = subElement.asType()
                val fieldName = subElement.simpleName.toString()

                when (fieldType.kind) {
                    TypeKind.BOOLEAN -> {
                        funcBuilder.addStatement("val %L = input.readBoolean()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.BYTE -> {
                        funcBuilder.addStatement("val %L = input.readByte()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.SHORT -> {
                        funcBuilder.addStatement("val %L = input.readShort()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.CHAR -> {
                        funcBuilder.addStatement("val %L = input.readChar()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.INT -> {
                        funcBuilder.addStatement("val %L = input.readInt()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.LONG -> {
                        funcBuilder.addStatement("val %L = input.readLong()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.FLOAT -> {
                        funcBuilder.addStatement("val %L = input.readFloat()",
                            fieldName
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.DOUBLE -> {
                        funcBuilder.addStatement("val %L = input.readDouble()",
                            fieldName,
                        )
                        constructorParameters.add(fieldName)
                    }
                    TypeKind.DECLARED -> {
                        val className = Class.forName(fieldType.toString()).kotlin
                        when (className.qualifiedName) {
                            "kotlin.String" -> {
                                funcBuilder.addStatement("val %L = input.readUTF()",
                                    fieldName
                                )
                                constructorParameters.add(fieldName)
                            }
                            "java.util.UUID" -> {
                                funcBuilder.addStatement("val %LBytes = ByteArray(16)", fieldName)
                                funcBuilder.addStatement("input.readFully(%LBytes)", fieldName)
                                funcBuilder.addStatement("val %L = %L.getUUIDFromBytes(%LBytes)", fieldName, ClassName("de.chaosolymp.portals.core", "UUIDUtils"), fieldName)
                                constructorParameters.add(fieldName)
                            }
                            else -> {
                                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Cannot process type ${className.qualifiedName}")
                                return null
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        funcBuilder.addStatement("return %L(${constructorParameters.joinToString(", ")})", element)

        return funcBuilder.build()
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}