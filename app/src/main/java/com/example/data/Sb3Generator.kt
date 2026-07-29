package com.example.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 将 Scratch 项目 JSON 动态包装为标准的 .sb3 (ZIP 压缩包) 字节流与 Base64
 * 包含 project.json 以及必要的默认空白矢量图 asset 文件，彻底防止 Scratch VM 充当纯 JSON 时发起远程素材下载网络阻塞
 */
object Sb3Generator {

    private const val DEFAULT_BACKGROUND_SVG = """<svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="480" height="360" viewBox="0 0 480 360"><rect width="480" height="360" fill="#ffffff"/></svg>"""
    private const val DEFAULT_CAT_COSTUME_SVG = """<svg version="1.1" xmlns="http://www.w3.org/2000/svg" width="96" height="100" viewBox="0 0 96 100"><path fill="#FFAB19" stroke="#000000" stroke-width="2" d="M20,30 Q30,10 50,20 Q70,10 80,30 Q90,60 50,90 Q10,60 20,30 Z"/><circle cx="35" cy="40" r="5" fill="#000"/><circle cx="65" cy="40" r="5" fill="#000"/><path d="M40,60 Q50,70 60,60" fill="none" stroke="#000" stroke-width="2"/></svg>"""

    /**
     * 规范并补全 Scratch 项目 JSON 数据
     */
    fun sanitizeProjectJson(rawJson: String): String {
        try {
            val root = if (rawJson.trim().startsWith("{")) JSONObject(rawJson) else JSONObject()
            if (!root.has("targets") || root.isNull("targets")) {
                root.put("targets", JSONArray())
            }
            val targets = root.getJSONArray("targets")

            var hasStage = false
            for (i in 0 until targets.length()) {
                val t = targets.optJSONObject(i) ?: continue
                if (t.optBoolean("isStage", false)) {
                    hasStage = true
                    break
                }
            }

            if (!hasStage) {
                val stage = JSONObject().apply {
                    put("isStage", true)
                    put("name", "Stage")
                    put("variables", JSONObject())
                    put("lists", JSONObject())
                    put("broadcasts", JSONObject())
                    put("blocks", JSONObject())
                    put("comments", JSONObject())
                    put("currentCostume", 0)
                    put("costumes", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "背景1")
                            put("bitmapResolution", 1)
                            put("dataFormat", "svg")
                            put("assetId", "cd21584322f79459ecb5864133b44723")
                            put("md5ext", "cd21584322f79459ecb5864133b44723.svg")
                            put("rotationCenterX", 240)
                            put("rotationCenterY", 180)
                        })
                    })
                    put("sounds", JSONArray())
                    put("volume", 100)
                    put("layerOrder", 0)
                }
                val newTargets = JSONArray()
                newTargets.put(stage)
                for (i in 0 until targets.length()) {
                    newTargets.put(targets.get(i))
                }
                root.put("targets", newTargets)
            }

            val updatedTargets = root.getJSONArray("targets")
            for (i in 0 until updatedTargets.length()) {
                val target = updatedTargets.optJSONObject(i) ?: continue
                if (!target.has("variables")) target.put("variables", JSONObject())
                if (!target.has("lists")) target.put("lists", JSONObject())
                if (!target.has("broadcasts")) target.put("broadcasts", JSONObject())
                if (!target.has("blocks")) target.put("blocks", JSONObject())
                if (!target.has("comments")) target.put("comments", JSONObject())
                if (!target.has("sounds")) target.put("sounds", JSONArray())
                if (!target.has("volume")) target.put("volume", 100)
                if (!target.has("layerOrder")) target.put("layerOrder", i)

                if (!target.has("costumes") || target.getJSONArray("costumes").length() == 0) {
                    val isStage = target.optBoolean("isStage", false)
                    target.put("costumes", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", if (isStage) "背景1" else "造型1")
                            put("bitmapResolution", 1)
                            put("dataFormat", "svg")
                            put("assetId", if (isStage) "cd21584322f79459ecb5864133b44723" else "b7853f557e44241d288a7593e62c0d58")
                            put("md5ext", if (isStage) "cd21584322f79459ecb5864133b44723.svg" else "b7853f557e44241d288a7593e62c0d58.svg")
                            put("rotationCenterX", if (isStage) 240 else 48)
                            put("rotationCenterY", if (isStage) 180 else 50)
                        })
                    })
                }

                if (!target.optBoolean("isStage", false)) {
                    if (!target.has("visible")) target.put("visible", true)
                    if (!target.has("x")) target.put("x", 0)
                    if (!target.has("y")) target.put("y", 0)
                    if (!target.has("size")) target.put("size", 100)
                    if (!target.has("direction")) target.put("direction", 90)
                    if (!target.has("draggable")) target.put("draggable", false)
                    if (!target.has("rotationStyle")) target.put("rotationStyle", "all around")
                }
            }

            if (!root.has("monitors")) root.put("monitors", JSONArray())
            if (!root.has("extensions")) root.put("extensions", JSONArray())
            if (!root.has("meta")) {
                root.put("meta", JSONObject().apply {
                    put("semver", "3.0.0")
                    put("vm", "0.2.0")
                    put("agent", "Android Sb3Generator")
                })
            }

            return root.toString()
        } catch (e: Exception) {
            return rawJson
        }
    }

    /**
     * 生成真实的 .sb3 ZIP 压缩包字节数组
     */
    fun createSb3ZipBytes(projectJson: String): ByteArray {
        val sanitizedJson = sanitizeProjectJson(projectJson)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. 写入 project.json
            val jsonEntry = ZipEntry("project.json")
            zos.putNextEntry(jsonEntry)
            zos.write(sanitizedJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. 写入默认背景资源 cd21584322f79459ecb5864133b44723.svg
            val bgEntry = ZipEntry("cd21584322f79459ecb5864133b44723.svg")
            zos.putNextEntry(bgEntry)
            zos.write(DEFAULT_BACKGROUND_SVG.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. 写入默认角色造型资源 b7853f557e44241d288a7593e62c0d58.svg
            val catEntry = ZipEntry("b7853f557e44241d288a7593e62c0d58.svg")
            zos.putNextEntry(catEntry)
            zos.write(DEFAULT_CAT_COSTUME_SVG.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    /**
     * 生成 Base64 编码的 .sb3 压缩包，方便传输给 JavaScript WebView
     */
    fun createSb3Base64(projectJson: String): String {
        val zipBytes = createSb3ZipBytes(projectJson)
        return Base64.encodeToString(zipBytes, Base64.NO_WRAP)
    }

    /**
     * 将 .sb3 压缩包保存至 File
     */
    fun writeSb3File(projectJson: String, targetFile: File) {
        val zipBytes = createSb3ZipBytes(projectJson)
        FileOutputStream(targetFile).use { fos ->
            fos.write(zipBytes)
        }
    }
}
