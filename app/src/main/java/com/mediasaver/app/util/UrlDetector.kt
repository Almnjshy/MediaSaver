package com.mediasaver.app.util

enum class Platform(val displayName: String) {
    YOUTUBE("YouTube"),
    TIKTOK("TikTok"),
    TWITTER("X / Twitter"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram"),
    REDDIT("Reddit"),
    SOUNDCLOUD("SoundCloud"),
    UNKNOWN("غير معروف")
}

object UrlDetector {

    private val urlRegex = Regex(
        "(https?://\\S+)",
        RegexOption.IGNORE_CASE
    )

    /** Pulls the first http(s) URL out of arbitrary shared text (e.g. share-sheet captions). */
    fun extractUrl(text: String): String? =
        urlRegex.find(text)?.value?.trim()

    fun detectPlatform(url: String): Platform {
        val host = runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: ""
        return when {
            "youtube" in host || "youtu.be" in host -> Platform.YOUTUBE
            "tiktok" in host -> Platform.TIKTOK
            "twitter" in host || host == "x.com" || "x.com" in host -> Platform.TWITTER
            "facebook" in host || "fb.watch" in host -> Platform.FACEBOOK
            "instagram" in host -> Platform.INSTAGRAM
            "reddit" in host -> Platform.REDDIT
            "soundcloud" in host -> Platform.SOUNDCLOUD
            else -> Platform.UNKNOWN
        }
    }

    fun isValidUrl(text: String): Boolean = urlRegex.containsMatchIn(text)
}
