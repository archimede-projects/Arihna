package com.archimedeprojects.arihna.feature.planning

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanningAndroidTest {
    @get:Rule val composeRule = createComposeRule()
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var repository: PlanningRepository
    private lateinit var scheduler: PlanningScheduler

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.filesDir, "datastore/planning-${UUID.randomUUID()}.preferences_pb")
        file.parentFile?.mkdirs()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { file }
        repository = PlanningRepository(store)
        scheduler = PlanningScheduler(context, repository)
    }

    @After fun tearDown() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
        file.delete()
    }

    @Test fun taskRoundTripEditCompletionAndDeletion() = runBlocking {
        val task = repository.save(
            PlanningType.TASK, "Comprare regalo", "per il 23 dicembre",
            null, null, AlarmSoundProfile.SILENT, null, null, 100,
        )
        assertEquals(1L, task.revision)
        assertEquals(task, repository.find(task.id))
        assertTrue(repository.setCompleted(task.id, true))
        assertFalse(requireNotNull(repository.find(task.id)).pending)
        assertTrue(repository.setCompleted(task.id, false))
        assertTrue(requireNotNull(repository.find(task.id)).pending)
        repository.delete(task.id)
        assertEquals(null, repository.find(task.id))
    }

    @Test fun reminderPersistsDateAndStaleDeliveryCannotCompleteUpdatedVersion() = runBlocking {
        val first = repository.save(
            PlanningType.REMINDER, "Telefonare", "al medico",
            LocalDate.of(2026, 12, 23), LocalTime.of(10, 0),
            AlarmSoundProfile.SILENT, null, null, 100,
        )
        val edited = repository.save(
            PlanningType.REMINDER, "Telefonare", "alle 11",
            LocalDate.of(2026, 12, 23), LocalTime.of(11, 0),
            AlarmSoundProfile.SILENT, null, null, 100, id = first.id,
        )
        assertEquals(2L, edited.revision)
        assertFalse(repository.markDelivered(first.id, first.revision))
        assertTrue(repository.markDelivered(edited.id, edited.revision))
        assertFalse(repository.markDelivered(edited.id, edited.revision))
    }

    @Test fun planningScreenShowsThreeEntryTypesAndCanCreateTask() {
        composeRule.setContent {
            ArihnaTheme {
                PlanningRoute(
                    contentPadding = PaddingValues(0.dp),
                    repository = repository, scheduler = scheduler, onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag("planning-root").assertIsDisplayed()
        composeRule.onNodeWithTag("planning-new-task").performClick()
        composeRule.onNodeWithTag("planning-editor").assertIsDisplayed()
        composeRule.onNodeWithTag("planning-editor-title").performTextInput("Comprare un regalo")
        composeRule.onNodeWithTag("planning-editor-save").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runBlocking { repository.all().any { it.title == "Comprare un regalo" } }
        }
        composeRule.onNodeWithText("Comprare un regalo").assertIsDisplayed()
    }

    @Test fun datedAlarmPersistsSpecificDateSoundAndIndependentVolume() = runBlocking {
        val item = repository.save(
            PlanningType.ALARM, "Partenza", "Viaggio",
            LocalDate.of(2026, 12, 23), LocalTime.of(6, 30),
            AlarmSoundProfile.SYSTEM_DEFAULT, "arihna://alarm/classic-vintage",
            "Sveglia classica Arihna", 42,
        )
        assertEquals(LocalDate.of(2026, 12, 23), repository.find(item.id)?.date)
        assertEquals(42, repository.find(item.id)?.volume)
        assertEquals("arihna://alarm/classic-vintage", repository.find(item.id)?.ringtoneUri)
    }
}
