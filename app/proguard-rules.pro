# kotlinx.serialization: keep generated serializers for backup models.
-keepclassmembers class com.goals.app.data.backup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
