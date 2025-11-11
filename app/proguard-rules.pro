# Plik: proguard-rules.pro

# Domyślne reguły (pozostają bez zmian)
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-keepattributes Signature,SourceFile,LineNumberTable,*Annotation*

# --- OSTATECZNE, NAJSILNIEJSZE REGUŁY DLA KRYTYCZNYCH KOMPONENTÓW ---

# 1. PEŁNA OCHRONA dla wszystkich komponentów systemowych i callbacków.
#    -keep mówi R8, aby nie usuwał, nie zmieniał nazw ORAZ NIE OPTYMALIZOWAŁ tych klas.
#    To jest najsilniejsza reguła i powinna rozwiązać problem.
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.bluetooth.le.ScanCallback { *; }
-keep class * extends android.bluetooth.BluetoothGattCallback { *; }
-keep class * extends android.bluetooth.BluetoothGattServerCallback { *; }
-keep class * extends android.bluetooth.le.AdvertiseCallback { *; }

# 2. Zachowaj klasy, które mają metody natywne (JNI).
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- REGUŁY SPECYFICZNE DLA APLIKACJI I BIBLIOTEK (mogą być mniej restrykcyjne) ---

# 3. Zachowaj klasy danych (data classes).
-keep class com.chudzikiewicz.smarthrfancontrol.ui.MainUiState { *; }
-keep class com.chudzikiewicz.smarthrfancontrol.core.preferences.** { *; }

# 4. Zachowaj publiczny interfejs SettingsManager.
-keep class com.chudzikiewicz.smarthrfancontrol.ui.SettingsManager {
    public *;
}

# 5. Reguły dla bibliotek zewnętrznych
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * { @kotlinx.serialization.Serializable <fields>; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn com.google.errorprone.annotations.**

# 6. Reguły dla Coroutines i Compose
-keep class kotlin.coroutines.jvm.internal.SuspendLambda { *; }
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl { <fields>; }
-keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
-keep class androidx.compose.runtime.internal.ComposableLambda { *; }