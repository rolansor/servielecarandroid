/**
 * SecurityUtils.kt - Utilidades de seguridad para hashing de contraseñas.
 *
 * Implementa hashing SHA-256 con salt aleatorio para almacenar contraseñas
 * de forma segura en la base de datos. El formato almacenado es `salt:hash`,
 * donde el salt es un arreglo de 16 bytes codificado en Base64.
 */
package com.example.serviaux.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SecurityUtils {
    /**
     * Genera un hash seguro de la contraseña con un salt aleatorio.
     * @param password Contraseña en texto plano.
     * @return Cadena en formato `saltBase64:hashHex` para almacenar en la BD.
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hash = sha256("$saltBase64:$password")
        return "$saltBase64:$hash"
    }

    /**
     * Verifica una contraseña contra su hash almacenado.
     * @param password Contraseña en texto plano a verificar.
     * @param storedHash Hash almacenado en formato `salt:hash`.
     * @return `true` si la contraseña coincide.
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0]
        val expectedHash = sha256("$salt:$password")
        return parts[1] == expectedHash
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
