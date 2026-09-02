package com.archimedeprojects.arihna.core.qibla.platform

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSensorEvent
import com.archimedeprojects.arihna.core.qibla.heading.HeadingSource
import com.archimedeprojects.arihna.core.qibla.heading.selectHeadingSource
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidHeadingSensorBackendAndroidTest {
    @Test
    fun capabilityDiscoveryAndCollectionCancellationAreApi28Safe() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = AndroidHeadingSensorBackend(context)
        val sources = backend.availableSources()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            assertFalse(sources.contains(HeadingSource.TRUE_HEADING_SENSOR))
        }

        val selected = selectHeadingSource(sources)
        if (selected != null) {
            val job = launch {
                backend.observe(selected).collect { }
            }
            delay(150)
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
        } else {
            val event = withTimeout(1_000) {
                backend.observe(HeadingSource.ROTATION_VECTOR).first()
            }
            assertTrue(event is HeadingSensorEvent.RegistrationFailed)
        }
    }
}
