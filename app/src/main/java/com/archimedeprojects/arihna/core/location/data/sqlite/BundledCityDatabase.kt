package com.archimedeprojects.arihna.core.location.data.sqlite

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Installs the immutable bundled city asset into app-private storage and opens it read-only.
 *
 * The APK stores the SQLite asset with DEFLATE compression, so Android's platform SQLite API
 * cannot open it in-place inside the APK. The first repository access copies it once to
 * noBackupFilesDir. The content-addressed filename and SHA-256 verification make a newer
 * bundled database coexist safely with an older installed copy after an app update.
 */
class BundledCityDatabase(context: Context) {
    private val appContext = context.applicationContext
    private val lock = Any()

    @Volatile
    private var openedDatabase: SQLiteDatabase? = null

    fun openReadOnly(): SQLiteDatabase {
        openedDatabase?.takeIf { it.isOpen }?.let { return it }

        return synchronized(lock) {
            openedDatabase?.takeIf { it.isOpen } ?: run {
                val databaseFile = ensureInstalled()
                SQLiteDatabase.openDatabase(
                    databaseFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
                ).also { database ->
                    database.execSQL("PRAGMA query_only=ON")
                    openedDatabase = database
                }
            }
        }
    }

    fun close() {
        synchronized(lock) {
            openedDatabase?.close()
            openedDatabase = null
        }
    }

    private fun ensureInstalled(): File {
        val directory = File(appContext.noBackupFilesDir, INSTALL_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create city database directory: $directory")
        }

        val target = File(directory, INSTALLED_FILE_NAME)
        if (isExpectedDatabase(target)) {
            return target
        }
        if (target.exists() && !target.delete()) {
            throw IOException("Unable to remove invalid city database: $target")
        }

        val temporary = File(directory, "$INSTALLED_FILE_NAME.tmp")
        if (temporary.exists() && !temporary.delete()) {
            throw IOException("Unable to remove stale city database temp file: $temporary")
        }

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var copiedBytes = 0L
            appContext.assets.open(ASSET_PATH, AssetManager.ACCESS_STREAMING).use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copiedBytes += read
                    }
                    output.fd.sync()
                }
            }

            val copiedSha = digest.digest().toHex()
            if (copiedBytes != EXPECTED_DATABASE_BYTES || copiedSha != EXPECTED_DATABASE_SHA256) {
                throw IOException(
                    "Bundled city database integrity mismatch: bytes=$copiedBytes sha256=$copiedSha",
                )
            }
            if (!temporary.renameTo(target)) {
                throw IOException("Unable to atomically install city database: $target")
            }
            return target
        } finally {
            if (temporary.exists()) {
                temporary.delete()
            }
        }
    }

    private fun isExpectedDatabase(file: File): Boolean =
        file.isFile &&
            file.length() == EXPECTED_DATABASE_BYTES &&
            sha256(file) == EXPECTED_DATABASE_SHA256

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    companion object {
        const val ASSET_PATH = "geonames/cities.db"
        const val EXPECTED_DATABASE_SHA256 =
            "6383538be045a51bbab6ae2e3097f99bdc79851af525c6bbc9fed018d434ce0a"
        const val EXPECTED_DATABASE_BYTES = 27_795_456L

        private const val INSTALL_DIRECTORY = "geonames"
        private const val INSTALLED_FILE_NAME =
            "cities-6383538be045a51bbab6ae2e3097f99b.db"
        private const val COPY_BUFFER_BYTES = 128 * 1024
    }
}
