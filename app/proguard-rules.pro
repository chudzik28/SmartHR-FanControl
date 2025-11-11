# Plik: proguard-rules.pro

# Domyślne reguły (pozostają bez zmian)
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-keepattributes Signature,SourceFile,LineNumberTable,*Annotation*

# --- REGUŁY DLA KOMPATYBILNOŚCI Z SYSTEMEM ANDROID ---

# 1. Zachowaj wszystkie klasy, które są komponentami systemu Android.
#    To chroni BroadcastReceivers, Services, Activities, etc. przed R8.
#    Ta reguła NAPRAWI potencjalny problem z BluetoothStateReceiver w wersji release.
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver { *; }
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper

# 2. Zachowaj wszystkie implementacje systemowych callbacków Bluetooth.
-keep public class * extends android.bluetooth.le.ScanCallback { *; }
-keep public class * extends android.bluetooth.BluetoothGattCallback { *; }
-keep public class * extends android.bluetooth.BluetoothGattServerCallback { *; }
-keep public class * extends android.bluetooth.le.AdvertiseCallback { *; }

# 3. Zachowaj klasy, które mają metody natywne (JNI).
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- REGUŁY SPECYFICZNE DLA APLIKACJI SMARTHR FANCONTROL ---

# 4. Zachowaj klasy danych (data classes).
-keep class com.chudzikiewicz.smarthrfancontrol.ui.MainUiState { *; }
-keep class com.chudzikiewicz.smarthrfancontrol.core.preferences.** { *; }

# 5. Zachowaj publiczny interfejs SettingsManager.
-keep class com.chudzikiewicz.smarthrfancontrol.ui.SettingsManager {
    public *;
}

# --- REGUŁY DLA BIBLIOTEK ZEWNĘTRZNYCH ---

# 6. Reguły dla bibliotek, które używają refleksji.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { @kotlinx.serialization.Serializable <fields>; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn com.google.errorprone.annotations.**

# 7. Reguły dla Coroutines i Compose.
-keep class kotlin.coroutines.jvm.internal.SuspendLambda { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl { <fields>; }
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }