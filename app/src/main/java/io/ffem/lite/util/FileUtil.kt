package io.ffem.lite.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object FileUtil {

    fun readTextFile(inputStream: InputStream): String {
        val sb = StringBuilder()
        var line: String?
        val br = BufferedReader(InputStreamReader(inputStream))
        line = br.readLine()
        while (line != null) {
            sb.append(line)
            line = br.readLine()
        }
        br.close()
        return sb.toString()
    }
}
