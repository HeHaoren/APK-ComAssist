# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep the application class
-keep class com.hehaoren.comassist.** { *; }

# Keep USB serial library classes
-keep class com.hoho.android.usbserial.** { *; }

# Keep Compose related classes
-keep class androidx.compose.** { *; }

# Keep data classes used for serialization
-keep class com.example.usart_connect.serial.SerialConfig { *; }
-keep class com.example.usart_connect.serial.DeviceItem { *; }
-keep class com.example.usart_connect.serial.QuickCommand { *; }
-keep class com.example.usart_connect.serial.NetworkDeviceInfo { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
