/**
 * SecurityUtils.kt - Utilidades de seguridad para hashing de contraseñas.
 *
 * Las contraseñas se guardan con **PBKDF2-HMAC-SHA256** y salt aleatorio, en el formato
 * `pbkdf2$<iteraciones>$<saltBase64>$<hashBase64>`.
 *
 * El formato anterior era un único SHA-256 (`saltBase64:hashHex`), que una GPU prueba a razón de
 * miles de millones de intentos por segundo: cualquiera con acceso a un archivo de respaldo podía
 * sacar las contraseñas del taller por fuerza bruta. PBKDF2 con muchas iteraciones hace ese
 * ataque inviable en la práctica.
 *
 * Los hashes antiguos se siguen aceptando al iniciar sesión y se migran al formato nuevo en ese
 * momento (ver [needsRehash]), sin pedir nada al usuario.
 */
package com.example.serviaux.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2"

    /**
     * Iteraciones de derivación. Es un compromiso: sube el costo para un atacante y también el
     * tiempo de login en el dispositivo (unos pocos cientos de milisegundos en un teléfono
     * modesto, una sola vez por inicio de sesión).
     */
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    /**
     * Genera el hash de una contraseña con salt aleatorio.
     * @return Cadena `pbkdf2$iteraciones$salt$hash` lista para guardar en la BD.
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        val encoder = Base64.getEncoder()
        return "$PREFIX$${ITERATIONS}$${encoder.encodeToString(salt)}$${encoder.encodeToString(hash)}"
    }

    /**
     * Verifica una contraseña contra su hash almacenado, en formato nuevo o antiguo.
     *
     * @param storedHash Hash guardado (`pbkdf2$...` o el legado `salt:hashHex`).
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (storedHash.startsWith("$PREFIX$")) {
            val parts = storedHash.split("$")
            // pbkdf2 $ iteraciones $ salt $ hash
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
            val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
            val actual = pbkdf2(password, salt, iterations)
            // Comparación en tiempo constante: no filtra cuántos bytes coincidieron.
            return MessageDigest.isEqual(expected, actual)
        }
        return verifyLegacyPassword(password, storedHash)
    }

    /**
     * True si el hash guardado usa el formato antiguo y conviene regenerarlo.
     * Se comprueba tras un login correcto, que es el único momento en el que se conoce la
     * contraseña en claro y por tanto se puede recalcular.
     */
    fun needsRehash(storedHash: String): Boolean = !storedHash.startsWith("$PREFIX$")

    /** Verificación del formato legado `saltBase64:hashHex` (SHA-256 de una sola pasada). */
    private fun verifyLegacyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val expected = sha256("${parts[0]}:$password")
        return MessageDigest.isEqual(parts[1].toByteArray(), expected.toByteArray())
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
