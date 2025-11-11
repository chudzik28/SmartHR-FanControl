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

# Plik: proguard-rules.pro

# Domyślne reguły Androida, które są zazwyczaj bezpieczne
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Zachowaj atrybuty potrzebne do debugowania i poprawnego działania refleksji
-keepattributes Signature,SourceFile,LineNumberTable,*Annotation*


# --- REGUŁY DLA KOMPATYBILNOŚCI Z SYSTEMEM ANDROID ---

# 1. Zachowaj klasy, które są punktami wejścia dla systemu Android (np. BroadcastReceiver).
#    To naprawi problem z nieodświeżaniem się statusu Bluetooth.
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper

# 2. Zachowaj wszystkie implementacje systemowych callbacków Bluetooth.
#    To naprawi problem z niedziałającym skanowaniem BLE w wersji release.
-keep public class * extends android.bluetooth.le.ScanCallback {
    public *;
}
-keep public class * extends android.bluetooth.BluetoothGattCallback {
    public *;
}
-keep public class * extends android.bluetooth.BluetoothGattServerCallback {
    public *;
}
-keep public class * extends android.bluetooth.le.AdvertiseCallback {
    public *;
}

# 3. Zachowaj klasy, które mają metody natywne (JNI).
#    To zapobiega usunięciu "mostu" do bibliotek .so.
-keepclasseswithmembernames class * {
    native <methods>;
}


# --- REGUŁY SPECYFICZNE DLA APLIKACJI SMARTHR FANCONTROL ---

# 4. Zachowaj WSZYSTKIE klasy danych (data classes) w projekcie.
#    To jest najważniejsza reguła, która chroni stan aplikacji.
-keep class com.chudzikiewicz.smarthrfancontrol.ui.MainUiState { *; }
-keep class com.chudzikiewicz.smarthrfancontrol.core.preferences.** { *; }

# 5. Zachowaj publiczny interfejs SettingsManager.
#    To chroni metody on...Changed przed zmianą nazwy.
-keep class com.chudzikiewicz.smarthrfancontrol.ui.SettingsManager {
    public *;
}


# --- REGUŁY DLA BIBLIOTEK ZEWNĘTRZNYCH ---

# 6. Reguły dla bibliotek, które używają refleksji (Ktor, Serialization).
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# 7. Reguły dla Coroutines.
-keep class kotlin.coroutines.jvm.internal.SuspendLambda { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <fields>;
}

# 8. Reguła dla biblioteki security-crypto.
-dontwarn com.google.errorprone.annotations.**

# 9. Reguły dla Jetpack Compose.
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }