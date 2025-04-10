package com.example.safefitness.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.safefitness.data.local.FitnessDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object DataExporter {
    suspend fun exportData(context: Context): File = withContext(Dispatchers.IO) {
        val db = FitnessDatabase.getDatabase(context)
        val data = db.fitnessDao().getAllData()
        val txtFile = File(context.getExternalFilesDir(null), "export.txt")
        BufferedWriter(FileWriter(txtFile)).use { writer ->
            writer.append("id,date,steps,heartRate")
            writer.newLine()
            data.forEach { item ->
                writer.append("${item.id},${item.date},${item.steps ?: ""},${item.heartRate ?: ""}")
                writer.newLine()
            }
        }
        txtFile
    }

    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share TXT"))
    }
}
