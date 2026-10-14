package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LeadZam", appName)
    }

    @Test
    fun testValidPhoneNumbers() {
        assertTrue(CallRequest.isValidPhoneNumber("+919876543210"))
        assertTrue(CallRequest.isValidPhoneNumber("+14155552671"))
        assertTrue(CallRequest.isValidPhoneNumber("9876543210"))
        assertTrue(CallRequest.isValidPhoneNumber("+91 98765 43210"))
        assertTrue(CallRequest.isValidPhoneNumber("(415) 555-2671"))
    }

    @Test
    fun testInvalidPhoneNumbers() {
        assertFalse(CallRequest.isValidPhoneNumber(""))
        assertFalse(CallRequest.isValidPhoneNumber(null))
        assertFalse(CallRequest.isValidPhoneNumber("123")) // Too short
        assertFalse(CallRequest.isValidPhoneNumber("abc1234567")) // Letters
        assertFalse(CallRequest.isValidPhoneNumber("call-me-now"))
    }

    @Test
    fun testSanitizeForDialer() {
        assertEquals("+919876543210", CallRequest.sanitizeForDialer("+91 98765-43210"))
        assertEquals("+14155552671", CallRequest.sanitizeForDialer("+1 (415) 555-2671"))
        assertEquals("9876543210", CallRequest.sanitizeForDialer("98765 43210"))
    }

    @Test
    fun testCallRequestRepository() {
        val request = CallRequest(
            phoneNumber = "+919876543210",
            contactName = "Rahul Sharma"
        )
        CallRequestRepository.postCallRequest(request)

        assertEquals("+919876543210", CallRequestRepository.activeRequest.value?.phoneNumber)
        assertEquals("Rahul Sharma", CallRequestRepository.activeRequest.value?.contactName)
        assertTrue(CallRequestRepository.requestHistory.value.isNotEmpty())

        CallRequestRepository.clearActiveRequest()
        assertEquals(null, CallRequestRepository.activeRequest.value)
    }
}
