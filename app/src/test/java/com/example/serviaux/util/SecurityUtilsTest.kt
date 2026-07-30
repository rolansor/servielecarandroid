package com.example.serviaux.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

/**
 * Pruebas del hashing de contraseñas.
 *
 * Corre en la JVM del equipo: [SecurityUtils] solo usa APIs de Java, sin dependencias de Android.
 */
class SecurityUtilsTest {

    @Test
    fun `la contrasena correcta se verifica`() {
        val hash = SecurityUtils.hashPassword("f4d3s2a1")
        assertTrue(SecurityUtils.verifyPassword("f4d3s2a1", hash))
    }

    @Test
    fun `una contrasena incorrecta se rechaza`() {
        val hash = SecurityUtils.hashPassword("f4d3s2a1")
        assertFalse(SecurityUtils.verifyPassword("f4d3s2a2", hash))
        assertFalse(SecurityUtils.verifyPassword("", hash))
        assertFalse(SecurityUtils.verifyPassword("F4D3S2A1", hash))
    }

    @Test
    fun `dos hashes de la misma contrasena son distintos por el salt`() {
        val primero = SecurityUtils.hashPassword("misma")
        val segundo = SecurityUtils.hashPassword("misma")
        assertNotEquals(primero, segundo)
        assertTrue(SecurityUtils.verifyPassword("misma", primero))
        assertTrue(SecurityUtils.verifyPassword("misma", segundo))
    }

    @Test
    fun `el hash nuevo usa PBKDF2 y no necesita migracion`() {
        val hash = SecurityUtils.hashPassword("clave")
        assertTrue(hash.startsWith("pbkdf2$"))
        assertFalse(SecurityUtils.needsRehash(hash))
    }

    @Test
    fun `los hashes del formato antiguo se siguen verificando`() {
        // Formato legado: saltBase64:sha256Hex("salt:password"), tal como lo generaba la versión
        // anterior y como está sembrado en seed_data.sql.
        val salt = Base64.getEncoder().encodeToString(ByteArray(16) { it.toByte() })
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt:secreta".toByteArray())
        val legacy = "$salt:${digest.joinToString("") { "%02x".format(it) }}"

        assertTrue(SecurityUtils.verifyPassword("secreta", legacy))
        assertFalse(SecurityUtils.verifyPassword("otra", legacy))
        assertTrue("un hash legado debe marcarse para migración", SecurityUtils.needsRehash(legacy))
    }

    @Test
    fun `un hash con formato invalido se rechaza sin lanzar`() {
        assertFalse(SecurityUtils.verifyPassword("x", ""))
        assertFalse(SecurityUtils.verifyPassword("x", "sin-separador"))
        assertFalse(SecurityUtils.verifyPassword("x", "pbkdf2\$120000\$soloTresPartes"))
        assertFalse(SecurityUtils.verifyPassword("x", "pbkdf2\$abc\$c2FsdA==\$aGFzaA=="))
    }

    @Test
    fun `el formato almacenado incluye las iteraciones`() {
        val partes = SecurityUtils.hashPassword("clave").split("$")
        assertEquals(4, partes.size)
        assertEquals("pbkdf2", partes[0])
        assertTrue("las iteraciones deben ser suficientes", partes[1].toInt() >= 100_000)
    }
}
