, "Terlambat: ${row.lateMinutes} menit", "Total diterima: ${rupiah(row.total)}").forEach { canvas.drawText(it, 45f, y, p); y += 24 }
    pdf.finishPage(page); val dir = File(context.cacheDir, "slip").apply { mkdirs() }; val file = File(dir, "slip_${row.employee.name.replace(" ", "_")}.pdf"); file.outputStream().use { pdf.writeTo(it) }; pdf.close()
    val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file); val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; context.startActivity(android.content.Intent.createChooser(intent, "Bagikan slip gaji"))
}
