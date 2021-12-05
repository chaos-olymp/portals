package de.chaosolymp.portals.core

import com.google.common.io.ByteStreams
import de.chaosolymp.portals.core.message.AbstractPluginMessage
import de.chaosolymp.portals.core.message.generated.deserialize
import de.chaosolymp.portals.core.message.generated.serialize
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Theories::class)
class MessageSerializationTests {
    companion object {
        @DataPoints
        @JvmField
        val data: List<AbstractPluginMessage> = sequence {
            val classes = getClasses("de.chaosolymp.portals.core.messages.proxy_to_server").toMutableList()
            classes.addAll(getClasses("de.chaosolymp.portals.core.messages.server_to_proxy"))

            classes.forEach { clazz ->
                clazz.declaredConstructors.forEach { constructor ->
                    val constructorParameters = mutableListOf<Any>()
                    constructor.parameterTypes.forEach { parameterType ->
                        when (parameterType) {
                            Byte::class.java -> constructorParameters.add(Random.nextInt(Byte.MAX_VALUE.toInt())
                                .toByte())
                            Short::class.java -> constructorParameters.add(Random.nextInt(Short.MAX_VALUE.toInt())
                                .toShort())
                            Int::class.java -> constructorParameters.add(Random.nextInt())
                            Long::class.java -> constructorParameters.add(Random.nextLong())
                            Boolean::class.java -> constructorParameters.add(Random.nextBoolean())
                            String::class.java -> {
                                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
                                val stringBuilder = StringBuilder()
                                for(i in 0 until Random.nextInt(100)) {
                                    stringBuilder.append(chars[Random.nextInt(chars.size)])
                                }
                                constructorParameters.add(stringBuilder.toString())
                            }
                            UUID::class.java -> constructorParameters.add(UUID.randomUUID())
                            else -> throw Exception("Cannot handle parameter of type $parameterType")
                        }
                    }

                    val instance = constructor.newInstance(*constructorParameters.toTypedArray())
                    yield(instance as AbstractPluginMessage)
                }
            }
        }.toList()

        /**
         * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
         *
         * @param packageName The base package
         * @return The classes
         * @throws ClassNotFoundException
         * @throws IOException
         */
        @Throws(ClassNotFoundException::class, IOException::class)
        private fun getClasses(packageName: String): Array<Class<*>> {
            val classLoader = Thread.currentThread().contextClassLoader!!
            val path = packageName.replace('.', '/')
            val resources: Enumeration<URL> = classLoader.getResources(path)
            val dirs: MutableList<File> = ArrayList<File>()
            while (resources.hasMoreElements()) {
                val resource: URL = resources.nextElement()
                dirs.add(File(resource.file))
            }
            val classes = ArrayList<Class<*>>()
            for (directory in dirs) {
                classes.addAll(findClasses(directory, packageName))
            }
            return classes.toTypedArray()
        }

        /**
         * Recursive method used to find all classes in a given directory and subdirectories.
         *
         * @param directory   The base directory
         * @param packageName The package name for classes found inside the base directory
         * @return The classes
         * @throws ClassNotFoundException
         */
        @Throws(ClassNotFoundException::class)
        private fun findClasses(directory: File, packageName: String): List<Class<*>> {
            val classes: MutableList<Class<*>> = ArrayList()
            if (!directory.exists()) {
                return classes
            }
            val files: Array<File> = directory.listFiles()!!
            for (file in files) {
                if (file.isDirectory) {
                    assert(!file.name.contains("."))
                    classes.addAll(findClasses(file, packageName + "." + file.name))
                } else if (file.name.endsWith(".class")) {
                    classes.add(
                        Class.forName(
                            packageName + '.' + file.name.substring(0, file.name.length - 6)
                        )
                    )
                }
            }
            return classes
        }
    }

    @Suppress("UnstableApiUsage")
    @Theory
    fun testSerializedAndDeserializedEquality(message: AbstractPluginMessage) {
        val output = ByteStreams.newDataOutput()
        serialize(message, output)
        val outputBytes = output.toByteArray()

        val input = ByteStreams.newDataInput(outputBytes)
        val deserialized = deserialize(input)

        assertNotNull(deserialized)
        assertDataEquality(message, deserialized)
    }

    private fun assertDataEquality(expected: AbstractPluginMessage, actual: AbstractPluginMessage) {
        AbstractPluginMessage::class.java.fields.forEach { field ->
            val expectedField = field.get(expected)
            val actualField = field.get(actual)

            assertEquals(expectedField, actualField)
        }
    }
}