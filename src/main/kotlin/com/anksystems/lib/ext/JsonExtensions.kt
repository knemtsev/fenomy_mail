package com.anksystems.lib.ext

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations

public inline fun <reified T> Json.decodeFromStringSafe(string: String?): T? {
    if (string == null)
        return null

    return tryOrNull(true) { decodeFromString(string) }
}

public inline fun <reified T> Json.encodeToStringSafe(value: T?): String? {
    if (value == null)
        return null

    return tryOrNull(true) { encodeToString(value) }
}

public fun <T> StringFormat.encodeToString(
    serializer: SerializationStrategy<T>,
    value: T,
    excludeClassDiscriminator: Boolean
): String {
    val json = this as Json

    return when (excludeClassDiscriminator) {
        false -> encodeToString(serializer, value)
        true -> {
            val json1 = json.encodeToJsonElement(serializer, value)

            val valuesWithoutDiscriminator = tryOrNull { json1.filterJsonElement(json) }
            json.encodeToString(valuesWithoutDiscriminator)
        }
    }
}

fun JsonElement.filterJsonElement(json: Json): JsonElement {
    val classDiscriminator = json.configuration.classDiscriminator

    return when (this) {
        is JsonObject -> { JsonObject(
            jsonObject
                .filterNot { it.key == classDiscriminator }
                .mapValues { it.value.filterJsonElement(json) }
        ) }

        is JsonPrimitive -> {
            if (this.contentOrNull?.contains(classDiscriminator) == true)
                TODO()
            else
                this
        }

        JsonNull -> JsonNull

        is JsonArray -> {
            JsonArray(mapNotNull { it.filterJsonElement(json) })
        }
    }
}


fun KProperty<*>.serializedName() =
    findAnnotation<SerialName>()
        ?.value

@OptIn(ExperimentalStdlibApi::class)
fun KProperty<*>.allSerializedNames() = findAnnotations<SerialName>().toList().map {
    it.value }

fun KProperty<*>.serializedName(defaultValue: String) =
    findAnnotation<SerialName>()
        ?.value ?: defaultValue