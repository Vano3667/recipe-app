# Debug build is not minified. Keep serialization models for release builds.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.example.homemenu.**$$serializer { *; }
-keepclassmembers class com.example.homemenu.** { *** Companion; }
