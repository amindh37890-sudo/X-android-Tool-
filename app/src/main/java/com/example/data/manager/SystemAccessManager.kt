package com.example.data.manager

import android.content.Context
import android.database.Cursor
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageSpaceInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val availableBytes: Long,
    val internalPath: String,
    val totalFormatted: String,
    val freeFormatted: String
)

data class MediaItemInfo(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val dateModified: Long,
    val uriString: String
)

data class AudioRecordTestResult(
    val isHardwareAvailable: Boolean,
    val sampleRate: Int,
    val channelConfig: String,
    val bufferSize: Int,
    val peakAmplitude: Int = 0,
    val message: String
)

class SystemAccessManager(private val context: Context) {

    /**
     * Inspects system root and external storage status, directories, and free capacity.
     */
    suspend fun getStorageOverview(): StorageSpaceInfo = withContext(Dispatchers.IO) {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val freeBytes = stat.freeBlocksLong * blockSize

        StorageSpaceInfo(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            availableBytes = availableBytes,
            internalPath = path.absolutePath,
            totalFormatted = formatFileSize(totalBytes),
            freeFormatted = formatFileSize(availableBytes)
        )
    }

    /**
     * Lists internal and external system files and folders for forensics and diagnostics.
     */
    suspend fun listSystemFiles(directoryPath: String): List<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(directoryPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches recent images from MediaStore for the media viewer / diagnostics panel.
     */
    suspend fun queryImages(limit: Int = 20): List<MediaItemInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemInfo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "image_$id"
                    val size = it.getLong(sizeCol)
                    val mime = it.getString(mimeCol)
                    val date = it.getLong(dateCol)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()

                    list.add(MediaItemInfo(id, name, size, mime, date, uri))
                    count++
                }
            }
        } catch (_: Exception) {
            // Permission or empty
        }
        list
    }

    /**
     * Fetches recent audio files from MediaStore for audio telemetry and diagnostics.
     */
    suspend fun queryAudioFiles(limit: Int = 20): List<MediaItemInfo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItemInfo>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: "audio_$id"
                    val size = it.getLong(sizeCol)
                    val mime = it.getString(mimeCol)
                    val date = it.getLong(dateCol)
                    val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()).toString()

                    list.add(MediaItemInfo(id, name, size, mime, date, uri))
                    count++
                }
            }
        } catch (_: Exception) {
            // Permission or empty
        }
        list
    }

    /**
     * Tests and verifies audio hardware input telemetry and sample buffers.
     */
    suspend fun sampleAudioHardware(): AudioRecordTestResult = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (minBufferSize <= 0) {
            return@withContext AudioRecordTestResult(
                isHardwareAvailable = false,
                sampleRate = sampleRate,
                channelConfig = "MONO",
                bufferSize = 0,
                message = "معدات الصوت لا تدعم الإعدادات الحالية"
            )
        }

        try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                return@withContext AudioRecordTestResult(
                    isHardwareAvailable = false,
                    sampleRate = sampleRate,
                    channelConfig = "MONO 16-BIT",
                    bufferSize = minBufferSize,
                    message = "تعذر تهيئة جهاز التسجيل (يحتاج صلاحية الميكروفون أو العتاد مشغول)"
                )
            }

            audioRecord.startRecording()
            val buffer = ShortArray(minBufferSize)
            val read = audioRecord.read(buffer, 0, minBufferSize)
            var maxAmp = 0
            if (read > 0) {
                for (i in 0 until read) {
                    val absVal = Math.abs(buffer[i].toInt())
                    if (absVal > maxAmp) maxAmp = absVal
                }
            }
            audioRecord.stop()
            audioRecord.release()

            AudioRecordTestResult(
                isHardwareAvailable = true,
                sampleRate = sampleRate,
                channelConfig = "44.1 kHz 16-BIT PCM",
                bufferSize = minBufferSize,
                peakAmplitude = maxAmp,
                message = "عتاد الصوت متصل ويعمل بنجاح (الذروة: $maxAmp)"
            )
        } catch (e: SecurityException) {
            AudioRecordTestResult(
                isHardwareAvailable = false,
                sampleRate = sampleRate,
                channelConfig = "MONO",
                bufferSize = minBufferSize,
                message = "صلاحية RECORD_AUDIO غير مفعلة من المستخدم"
            )
        } catch (e: Exception) {
            AudioRecordTestResult(
                isHardwareAvailable = false,
                sampleRate = sampleRate,
                channelConfig = "MONO",
                bufferSize = minBufferSize,
                message = "خطأ في عتاد الصوت: ${e.message}"
            )
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val df = java.text.DecimalFormat("#.##")
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${df.format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
            bytes >= 1024 * 1024 -> "${df.format(bytes.toDouble() / (1024 * 1024))} MB"
            bytes >= 1024 -> "${df.format(bytes.toDouble() / 1024)} KB"
            else -> "$bytes B"
        }
    }
}
