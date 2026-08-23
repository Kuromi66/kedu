# Keep backup models if future JSON serializers are added.
# Current release build relies on AndroidX/Compose default keep rules.

# kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.pulse.checkin.**$$serializer { *; }
-keepclassmembers class com.pulse.checkin.** {
    *** Companion;
}
-keepclasseswithmembers class com.pulse.checkin.** {
    kotlinx.serialization.KSerializer serializer(...);
}
