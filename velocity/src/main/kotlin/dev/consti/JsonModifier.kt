package dev.consti

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

object JsonModifier {
    fun updateJsonVersion(path: Path, version: String) {
        if (!Files.exists(path)) return

        val content = Files.readString(path)
        val jsonObject = JsonParser.parseString(content).asJsonObject
        jsonObject.addProperty("version", version)
        Files.writeString(path, jsonObject.toString())
    }
}
