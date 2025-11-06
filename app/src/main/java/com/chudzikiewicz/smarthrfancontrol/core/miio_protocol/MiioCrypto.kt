/*
 * SmartHR FanControl
 * Copyright (C) [2025] [Marcin Chudzikiewicz]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.chudzikiewicz.smarthrfancontrol.core.miio_protocol

import mu.KotlinLogging
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

object MiioCrypto {

    private const val AES_ALGORITHM_MODE = "AES/CBC/NoPadding"
    private const val MD5_ALGORITHM = "MD5"
    private const val AES_BLOCK_SIZE = 16

    /**
     * Generates AES and IV from token Miio according to protocol standard
     * Key AES: MD5(token)
     * IV: MD5(Key AES + token)
     */
    fun generateKeyAndIv(token: String): Pair<SecretKeySpec, IvParameterSpec> {
        require(token.length == 32) { "Token must be 32 signs HEX." }
        val tokenBytes = token.decodeHex()


        val keyBytes = md5(tokenBytes)
        val key = SecretKeySpec(keyBytes, "AES")


        val ivBytes = md5(keyBytes + tokenBytes)
        val iv = IvParameterSpec(ivBytes)

        logger.debug { "Generated AES Key: ${keyBytes.toHex()}, IV: ${ivBytes.toHex()}" }
        return Pair(key, iv)
    }


    fun encrypt(data: ByteArray, key: SecretKeySpec, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(padPkcs7(data))
    }


    fun decrypt(data: ByteArray, key: SecretKeySpec, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance(AES_ALGORITHM_MODE)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val decrypted = cipher.doFinal(data)
        // Czasem urządzenia zwracają puste, niepoprawnie dopełnione dane, stąd dodana walidacja
        return if (decrypted.isNotEmpty()) unpadPkcs7(decrypted) else decrypted
    }


    fun calculateChecksum(
        headerFragment: ByteArray,
        payload: ByteArray,
        token: ByteArray
    ): ByteArray {
        val digest = MessageDigest.getInstance(MD5_ALGORITHM)
        digest.update(headerFragment)
        // Suma kontrolna odpowiedzi z urządzenia nie zawiera payloadu
        if (payload.isNotEmpty()) {
            digest.update(payload)
        }
        digest.update(token) // Token jest kluczowym elementem checksum
        return digest.digest()
    }

    private fun padPkcs7(data: ByteArray): ByteArray {
        val padding = AES_BLOCK_SIZE - (data.size % AES_BLOCK_SIZE)
        val padded = ByteArray(data.size + padding)
        System.arraycopy(data, 0, padded, 0, data.size)
        for (i in data.size until padded.size) {
            padded[i] = padding.toByte()
        }
        return padded
    }

    private fun unpadPkcs7(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val padding = data.last().toInt() and 0xFF // Poprawka na signed byte
        if (padding <= 0 || padding > data.size || padding > AES_BLOCK_SIZE) {
            return data
        }
        for (i in (data.size - padding) until data.size) {
            if (data[i] != padding.toByte()) {
                return data
            }
        }
        return data.copyOfRange(0, data.size - padding)
    }

    private fun md5(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(MD5_ALGORITHM)
        return digest.digest(data)
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}