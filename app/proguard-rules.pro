# Reglas de R8 para Serviaux.
#
# El build de release ofusca y elimina código no usado (isMinifyEnabled). Estas reglas protegen
# lo que se resuelve por reflexión en tiempo de ejecución y que R8 no puede detectar leyendo el
# código: sin ellas la app compila igual pero falla al usarla.

# ── Trazas de error legibles ──────────────────────────────────────────────
# Sin esto, un crash en producción no dice en qué línea ocurrió.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Dropbox SDK ───────────────────────────────────────────────────────────
# Serializa y deserializa sus modelos con Jackson por reflexión sobre nombres de campos:
# ofuscarlos rompe la autenticación y la subida de respaldos.
-keep class com.dropbox.core.** { *; }
-keep interface com.dropbox.core.** { *; }
-dontwarn com.dropbox.core.**

-keep class com.fasterxml.jackson.** { *; }
-keepclassmembers class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Jackson y el SDK referencian APIs de servidor y anotaciones que no existen en Android.
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn javax.servlet.**
-dontwarn org.w3c.dom.bootstrap.**
-dontwarn com.google.appengine.**

# ── Room ──────────────────────────────────────────────────────────────────
# Room genera sus implementaciones en tiempo de compilación y trae sus propias reglas, pero las
# entidades y proyecciones se instancian desde el código generado: se conservan sus miembros.
-keep class com.example.serviaux.data.entity.** { *; }
-keep class com.example.serviaux.data.dao.** { *; }

# Los TypeConverters resuelven enums por nombre (Enum.valueOf).
-keepclassmembers enum com.example.serviaux.data.entity.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Modelos de respaldo ───────────────────────────────────────────────────
# El respaldo se serializa a JSON con org.json usando nombres de campo explícitos, así que no
# depende de reflexión; se conservan igualmente los tipos que cruzan la frontera del ZIP.
-keep class com.example.serviaux.repository.BackupCategory { *; }
-keep class com.example.serviaux.repository.BackupContent { *; }

# ── Biometría ─────────────────────────────────────────────────────────────
-dontwarn androidx.biometric.**
