# Pocket Familiar ProGuard rules

# Keep DataStore Preferences keys
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences$Key { *; }

# Suppress warnings for unused classes in release
-dontwarn kotlin.Unit
