# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Plik: proguard-rules.pro

# Plik: proguard-rules.pro

# Domyślne reguły Androida, które są zazwyczaj bezpieczne
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Zachowaj atrybuty potrzebne do debugowania i poprawnego działania refleksji
-keepattributes Signature,SourceFile,LineNumberTable,*Annotation*

# --- OSTATECZNE, NIEZAWODNE REGUŁY DLA SMARTHR FANCONTROL ---

# 1. Zachowaj WSZYSTKIE klasy danych (data classes) w projekcie.
# To jest najważniejsza reguła, która chroni stan aplikacji.
-keep class com.chudzikiewicz.smarthrfancontrol.ui.MainUiState { *; }
-keep class com.chudzikiewicz.smarthrfancontrol.core.preferences.** { *; }

# 2. Zachowaj publiczny interfejs SettingsManager.
# To chroni metody on...Changed przed zmianą nazwy.
-keep class com.chudzikiewicz.smarthrfancontrol.ui.SettingsManager {
    public *;
}

# 3. Reguły dla bibliotek, które używają refleksji.
# Te reguły są zalecane w ich oficjalnej dokumentacji.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# 4. Reguły dla Coroutines.
-keep class kotlin.coroutines.jvm.internal.SuspendLambda { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <fields>;
}

# 5. Reguła dla biblioteki security-crypto.
-dontwarn com.google.errorprone.annotations.**

# 6. Reguły dla Jetpack Compose.
# Chronią one kod generowany przez kompilator Compose.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }