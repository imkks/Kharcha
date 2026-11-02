# Kharcha ProGuard / R8 Rules

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room Database & SQLCipher
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep domain models and JSON template entities
-keep class com.kharcha.core.model.** { *; }
-keep class com.kharcha.engine.parser.Template { *; }
-keep class com.kharcha.engine.parser.TemplateRule { *; }
-keep class com.kharcha.engine.parser.TemplateCatalog { *; }
