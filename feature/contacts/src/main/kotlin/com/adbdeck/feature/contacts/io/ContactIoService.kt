package com.adbdeck.feature.contacts.io

import java.io.File

/**
 * Сервис для чтения и записи файлов контактов (JSON, VCF) на локальном диске.
 *
 * Использует стандартные JVM I/O-операции. Кодировка — UTF-8.
 */
object ContactIoService {

    /**
     * Записать текстовый контент в файл по указанному пути.
     *
     * @param path    Абсолютный путь к файлу.
     * @param content Текст для записи.
     * @throws java.io.IOException при ошибке ввода-вывода.
     */
    fun writeText(path: String, content: String) {
        File(path).writeText(content, Charsets.UTF_8)
    }

    /**
     * Прочитать текстовый файл по указанному пути.
     *
     * @param path Абсолютный путь к файлу.
     * @return Содержимое файла.
     * @throws java.io.IOException при ошибке ввода-вывода.
     */
    fun readText(path: String): String =
        File(path).readText(Charsets.UTF_8)
}
