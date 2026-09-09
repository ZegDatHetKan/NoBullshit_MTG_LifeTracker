# kotlinx.serialization keeps generated serializers for @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.nobs.mtglifetracker.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.nobs.mtglifetracker.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
