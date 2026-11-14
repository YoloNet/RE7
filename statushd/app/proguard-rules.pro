# StatusHD — ProGuard rules (release).
# FFmpegKit: keep its public API (it is called by reflection-free Java API,
# but the native bridge classes must survive shrinking).
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }
