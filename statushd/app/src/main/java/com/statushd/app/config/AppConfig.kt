package com.statushd.app.config

/** StatusHD — pemalar aplikasi (bukan rahsia; tiada kunci di sini). */
object AppConfig {
    const val APP_NAME = "StatusHD"
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1

    /** Panjang satu bahagian splitter (ms) — had Status WhatsApp. */
    const val SPLIT_PART_MS = 30_000L

    /** Tempoh video daripada foto (saat). */
    const val PHOTO_VIDEO_SECONDS = 5L

    /** Folder output dalam cache + MediaStore. */
    const val OUTPUT_DIR = "statushd"
    const val GALLERY_FOLDER = "StatusHD"

    /** Pakej WhatsApp (utama + Business) untuk kongsi. */
    val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** E-mel sokongan — TODO (Jean): isi alamat sebenar sebelum pelancaran. */
    const val SUPPORT_EMAIL = "support@statushd.app"

    /** Had pertanyaan MediaStore (item terkini). */
    const val MEDIA_QUERY_LIMIT = 500
}
