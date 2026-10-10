package com.pabi277.messbillcheck

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "we collect nothing" claim is enforced by machinery, not memory
 * (research plan SS9 and SS14). AGP runs unit tests with the module directory as the
 * working directory, so this reads the shipping manifest of exactly this build.
 */
class PrivacyContractTest {

    private val manifest: File = File("src/main/AndroidManifest.xml")

    @Test
    fun `manifest exists and is readable`() {
        assertTrue("expected ${manifest.absolutePath} to exist", manifest.isFile)
    }

    @Test
    fun `v1 requests no internet permission`() {
        val text = manifest.readText()
        assertFalse(
            "v1 is offline by construction; INTERNET must not appear in the manifest",
            text.contains("android.permission.INTERNET")
        )
    }

    @Test
    fun `v1 requests no account, contacts, sms, camera or location permissions`() {
        val text = manifest.readText()
        val banned = listOf(
            "GET_ACCOUNTS", "MANAGE_ACCOUNTS", "READ_CONTACTS", "READ_SMS",
            "CAMERA", "ACCESS_FINE_LOCATION", "READ_EXTERNAL_STORAGE"
        )
        for (permission in banned) {
            assertFalse("unexpected permission in v1 manifest: $permission", text.contains(permission))
        }
    }
}
