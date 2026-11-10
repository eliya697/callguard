# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.eliya.callguard.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
