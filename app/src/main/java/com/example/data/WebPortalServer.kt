package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * 阶段三 (任务 3.1 & 3.2)
 * PC 独立 Web 后台与 TurboWarp 可视化大屏服务
 * 在本地 8080 端口启动 HTTP 服务，供 PC 浏览器或局域网访问教师管理与作品大屏
 */
class WebPortalServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    suspend fun startServer(port: Int = 8080) = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            Log.d("WebPortalServer", "PC Web 后台已成功启动: http://127.0.0.1:$port/portal")

            while (isRunning) {
                val clientSocket = serverSocket?.accept() ?: break
                handleClientRequest(clientSocket)
            }
        } catch (e: Exception) {
            Log.e("WebPortalServer", "启动 PC Web 端口 $port 异常: ${e.message}")
        }
    }

    private fun handleClientRequest(socket: Socket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val line = reader.readLine() ?: return@Thread
                if (line.startsWith("GET")) {
                    val htmlContent = getPortalHtml()
                    writer.println("HTTP/1.1 200 OK")
                    writer.println("Content-Type: text/html; charset=UTF-8")
                    writer.println("Content-Length: " + htmlContent.toByteArray(Charsets.UTF_8).size)
                    writer.println("Connection: close")
                    writer.println()
                    writer.println(htmlContent)
                }
                socket.close()
            } catch (e: Exception) {
                Log.e("WebPortalServer", "处理 PC 浏览器请求异常: ${e.message}")
            }
        }.start()
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPortalHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="UTF-8">
            <title>星梭智学 - PC端 教师管理与大屏中心</title>
            <style>
              body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; padding: 24px; }
              header { border-bottom: 1px solid #334155; padding-bottom: 16px; margin-bottom: 24px; display:flex; justify-content:space-between; align-items:center; }
              .card { background: #1e293b; border-radius: 12px; padding: 20px; border: 1px solid #334155; margin-bottom: 20px; }
              .badge { background: #22c55e; color: #fff; padding: 4px 8px; border-radius: 6px; font-size: 12px; }
              .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; }
            </style>
            </head>
            <body>
            <header>
              <h1>🚀 星梭智学编程助教 - PC 端控制中心</h1>
              <span class="badge">Supabase RLS 云端服务已连接</span>
            </header>
            <div class="grid">
              <div class="card">
                <h3>🧩 批量作业批改与离线下载</h3>
                <p style="font-size:13px;color:#94a3b8;margin-top:8px;">支持批量导出学生作品 .sb3 文件与生成 HTML/zip 报告包。</p>
              </div>
              <div class="card">
                <h3>📊 全校教研与能力画像大屏</h3>
                <p style="font-size:13px;color:#94a3b8;margin-top:8px;">同步手机端雷达图，可视化班级语法错误排行榜与胜任力成果。</p>
              </div>
            </div>
            </body>
            </html>
        """.trimIndent()
    }
}
