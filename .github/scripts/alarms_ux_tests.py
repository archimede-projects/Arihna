from pathlib import Path


def write(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")


write("app/src/test/java/com/archimedeprojects/arihna/feature/alarms/data/preferences/AlarmRulePreferencesCodecTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms.data.preferences

import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRulePreferencesCodecTest {
    @Test
    fun emptyCollectionHasStableV2EncodingAndV1StillDecodes() {
        assertEquals("ARIHNA_ALARMS_V2", AlarmRulePreferencesCodec.encode(emptyList()))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V2"))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V1"))
        assertEquals(emptyList<AlarmRule>(), AlarmRulePreferencesCodec.decode(null))
    }

    @Test
    fun prayerAndCustomRulesRoundTripRingtoneSelectionLosslessly() {
        val rules = listOf(
            AlarmRule(
                alarmId = "prayer|fajr/à",
                revision = 19,
                enabled = true,
                soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR, -35),
                ringtoneUri = "content://media/alarm/17",
                ringtoneTitle = "Morning Flower",
            ),
            AlarmRule(
                alarmId = "custom:work",
                revision = 3,
                enabled = false,
                soundProfile = AlarmSoundProfile.SILENT,
                definition = AlarmDefinition.Custom(
                    label = "Lavoro | الرياض",
                    localTime = LocalTime.of(6, 45, 12),
                    weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                ),
            ),
        )

        val encoded = AlarmRulePreferencesCodec.encode(rules)
        assertTrue(encoded.startsWith("ARIHNA_ALARMS_V2\n"))
        assertEquals(rules.sortedBy { it.alarmId }, AlarmRulePreferencesCodec.decode(encoded))
    }

    @Test
    fun v1RulesMigrateWithoutInventingRingtone() {
        val oldSystem = "ARIHNA_ALARMS_V1\nP|cHJheWVyLWZhanI|1|1|SYSTEM_DEFAULT|FAJR|0"
        val decoded = AlarmRulePreferencesCodec.decode(oldSystem).single()
        assertEquals(AlarmSoundProfile.SYSTEM_DEFAULT, decoded.soundProfile)
        assertNull(decoded.ringtoneUri)
        assertNull(decoded.ringtoneTitle)
    }

    @Test
    fun collectionEncodingIsDeterministicRegardlessOfInputOrder() {
        val first = prayer("z", AlarmPrayer.ISHA)
        val second = prayer("a", AlarmPrayer.DHUHR)
        assertEquals(
            AlarmRulePreferencesCodec.encode(listOf(first, second)),
            AlarmRulePreferencesCodec.encode(listOf(second, first)),
        )
    }

    @Test(expected = AlarmRulesPersistenceException::class)
    fun rejectsUnknownVersionRatherThanSilentlyDroppingRules() {
        AlarmRulePreferencesCodec.decode("ARIHNA_ALARMS_V3")
    }

    @Test(expected = AlarmRulesPersistenceException::class)
    fun rejectsDuplicateIds() {
        AlarmRulePreferencesCodec.encode(listOf(prayer("same", AlarmPrayer.FAJR), prayer("same", AlarmPrayer.ISHA)))
    }

    @Test
    fun weekdayEncodingUsesStableIsoOrdering() {
        val encoded = AlarmRulePreferencesCodec.encode(
            listOf(
                AlarmRule(
                    alarmId = "weekdays",
                    revision = 1,
                    enabled = true,
                    soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
                    definition = AlarmDefinition.Custom(
                        label = "Weekdays",
                        localTime = LocalTime.of(8, 0),
                        weekdays = setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.MONDAY),
                    ),
                ),
            ),
        )
        assertTrue(encoded.contains("|1,2,7|-|-"))
    }

    private fun prayer(id: String, prayer: AlarmPrayer) = AlarmRule(
        alarmId = id,
        revision = 1,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.PrayerLinked(prayer),
    )
}
''')

write("app/src/test/java/com/archimedeprojects/arihna/feature/alarms/AlarmsViewModelTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms

import com.archimedeprojects.arihna.feature.alarms.data.AlarmRuleRepository
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayerScheduleSource
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmReconciler
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRuleDraft
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmNotificationPermissionReader
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmPlatformScheduler
import com.archimedeprojects.arihna.feature.alarms.platform.AlarmScheduleResult
import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmCapability
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun customCreationPersistsValidatedClockValueBeforeScheduling() = runTest(dispatcher) {
        val events = mutableListOf<String>()
        val repository = MutableRepository(events)
        val scheduler = RecordingScheduler(events)
        val viewModel = viewModel(repository, scheduler)

        viewModel.saveCustom(
            existing = null,
            label = "Farmaco",
            localTime = LocalTime.of(12, 0),
            weekdays = emptySet(),
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            ringtoneUri = "content://alarm/4",
            ringtoneTitle = "Morning Flower",
        )
        advanceUntilIdle()

        assertEquals(listOf("save", "schedule"), events)
        val saved = repository.items.value.single()
        val definition = saved.definition as AlarmDefinition.Custom
        assertEquals(LocalTime.NOON, definition.localTime)
        assertEquals("Morning Flower", saved.ringtoneTitle)
    }

    @Test
    fun editingPreservesAlarmIdAdvancesRevisionCancelsOldOccurrenceAndReconciles() = runTest(dispatcher) {
        val events = mutableListOf<String>()
        val existing = AlarmRule(
            alarmId = "custom-stable",
            revision = 7,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom("Lavoro", LocalTime.of(6, 30)),
        )
        val repository = MutableRepository(events, listOf(existing))
        val scheduler = RecordingScheduler(events)
        val viewModel = viewModel(repository, scheduler)

        viewModel.saveCustom(
            existing = existing,
            label = "Lavoro nuovo",
            localTime = LocalTime.of(7, 15),
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            soundProfile = AlarmSoundProfile.ADHAN,
            ringtoneUri = null,
            ringtoneTitle = null,
        )
        advanceUntilIdle()

        val updated = repository.items.value.single()
        assertEquals("custom-stable", updated.alarmId)
        assertEquals(8L, updated.revision)
        assertNotEquals(existing.revision, updated.revision)
        assertEquals(AlarmSoundProfile.ADHAN, updated.soundProfile)
        assertEquals(listOf("save", "cancel:custom-stable", "schedule"), events)
    }

    private fun viewModel(repository: MutableRepository, scheduler: RecordingScheduler): AlarmsViewModel {
        val reconciler = AlarmReconciler(
            ruleRepository = repository,
            platformScheduler = scheduler,
            prayerScheduleSource = AlarmPrayerScheduleSource { null },
            clock = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC),
            deviceZoneId = { ZoneId.of("UTC") },
            deliveryReady = { true },
        )
        return AlarmsViewModel(
            repository = repository,
            reconciler = reconciler,
            scheduler = scheduler,
            notificationPermissionReader = AlarmNotificationPermissionReader { true },
        )
    }

    private class MutableRepository(
        private val events: MutableList<String>,
        initial: List<AlarmRule> = emptyList(),
    ) : AlarmRuleRepository {
        val items = MutableStateFlow(initial)
        override val rules: Flow<List<AlarmRule>> = items
        override suspend fun getAll(): List<AlarmRule> = items.value
        override suspend fun get(alarmId: String): AlarmRule? = items.value.firstOrNull { it.alarmId == alarmId }

        override suspend fun save(draft: AlarmRuleDraft): AlarmRule {
            events += "save"
            val existing = items.value.firstOrNull { it.alarmId == draft.alarmId }
            val saved = AlarmRule(
                alarmId = draft.alarmId,
                revision = (existing?.revision ?: 0L) + 1L,
                enabled = draft.enabled,
                soundProfile = draft.soundProfile,
                definition = draft.definition,
                ringtoneUri = draft.ringtoneUri,
                ringtoneTitle = draft.ringtoneTitle,
            )
            items.value = items.value.filterNot { it.alarmId == saved.alarmId } + saved
            return saved
        }

        override suspend fun setEnabled(alarmId: String, enabled: Boolean): AlarmRule? = error("not used")
        override suspend fun delete(alarmId: String): AlarmRule? = error("not used")
        override suspend fun disableOneShotAfterValidatedDelivery(alarmId: String, expectedRevision: Long): AlarmRule? = error("not used")
    }

    private class RecordingScheduler(private val events: MutableList<String>) : AlarmPlatformScheduler {
        override fun capability(): ExactAlarmCapability = ExactAlarmCapability.READY
        override fun scheduleExact(occurrence: AlarmOccurrence): AlarmScheduleResult {
            events += "schedule"
            return AlarmScheduleResult.Scheduled(occurrence)
        }
        override fun cancel(alarmId: String) { events += "cancel:$alarmId" }
    }
}
''')

write("app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/AlarmsScreenAndroidTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmPrayer
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlarmsScreenAndroidTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun alarmsSurfaceContainsOnlyPersonalAlarmsAndNoCapabilityOrPrayerPanel() {
        val custom = customRule()
        val prayer = AlarmRule(
            alarmId = "prayer-fajr",
            revision = 1,
            enabled = true,
            soundProfile = AlarmSoundProfile.ADHAN,
            definition = AlarmDefinition.PrayerLinked(AlarmPrayer.FAJR),
        )
        setScreen(AlarmsUiState(rules = listOf(custom, prayer)))

        composeRule.onNodeWithTag("alarms-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("alarm-custom-row-custom-visible").assertIsDisplayed()
        composeRule.onNodeWithText("Farmaco").assertIsDisplayed()
        assertTextAbsent("Prontezza sveglie")
        assertTextAbsent("Preghiere")
        assertTextAbsent("Fajr")
    }

    @Test
    fun newAlarmAndEditAreExplicitTapTargets() {
        val custom = customRule()
        var newClicks = 0
        var edited: AlarmRule? = null
        setScreen(
            state = AlarmsUiState(rules = listOf(custom)),
            onNew = { newClicks += 1 },
            onEdit = { edited = it },
        )

        composeRule.onNodeWithTag("alarm-new").performClick()
        composeRule.onNodeWithTag("alarm-edit-custom-visible").performClick()
        composeRule.runOnIdle {
            assertEquals(1, newClicks)
            assertEquals("custom-visible", edited?.alarmId)
        }
    }

    @Test
    fun selectedPhoneRingtoneNameIsVisibleOnPersonalAlarmCard() {
        val custom = customRule().copy(
            ringtoneUri = "content://alarm/42",
            ringtoneTitle = "Morning Flower",
        )
        setScreen(AlarmsUiState(rules = listOf(custom)))
        composeRule.onNodeWithText("Morning Flower").assertIsDisplayed()
    }

    private fun customRule() = AlarmRule(
        alarmId = "custom-visible",
        revision = 3,
        enabled = true,
        soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
        definition = AlarmDefinition.Custom(
            label = "Farmaco",
            localTime = LocalTime.of(7, 30),
        ),
    )

    private fun setScreen(
        state: AlarmsUiState,
        onNew: () -> Unit = {},
        onEdit: (AlarmRule) -> Unit = {},
    ) {
        composeRule.setContent {
            ArihnaTheme {
                AlarmsScreen(
                    contentPadding = PaddingValues(0.dp),
                    state = state,
                    onNew = onNew,
                    onEdit = onEdit,
                    onToggle = {},
                    onDelete = {},
                )
            }
        }
    }

    private fun assertTextAbsent(text: String) {
        assertTrue(composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
    }
}
''')

write("app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmNotificationAndroidTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmDefinition
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmOccurrence
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmRule
import com.archimedeprojects.arihna.feature.alarms.domain.AlarmSoundProfile
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmNotificationAndroidTest {
    @Test
    fun api28CreatesHighImportanceSilentChannelsAndBuildsFullScreenAlarmNotification() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AlarmRingingNotificationFactory.ensureChannels(context)

        val prayerChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_PRAYER)
        val customChannel = manager.getNotificationChannel(AlarmRingingNotificationFactory.CHANNEL_CUSTOM)
        assertNotNull(prayerChannel)
        assertNotNull(customChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, prayerChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, customChannel.importance)
        assertNull(prayerChannel.sound)
        assertNull(customChannel.sound)

        val payload = AlarmRingingPayload(
            alarmId = "instrumented-notification",
            title = "Fajr",
            soundProfile = AlarmSoundProfile.ADHAN,
            isPrayer = true,
            occurrenceToken = "instrumented",
        )
        val notification = AlarmRingingNotificationFactory.build(context, payload)
        assertNotNull(notification.fullScreenIntent)
        assertNotNull(notification.contentIntent)
        assertEquals(1, notification.actions.size)
    }

    @Test
    fun validatedDeliveryPostsUrgentNotificationBeforeStartingRingingExactlyOnce() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val events = mutableListOf<String>()
        val delivery = AndroidAlarmNotificationDelivery(
            context = context,
            permissionReader = AlarmNotificationPermissionReader { true },
            ringingStarter = AlarmRingingStarter { _, _ -> events += "start" },
            notificationPoster = AlarmNotificationPoster { _, _ -> events += "post" },
        )
        val rule = AlarmRule(
            alarmId = "instrumented-handoff",
            revision = 1,
            enabled = true,
            soundProfile = AlarmSoundProfile.SYSTEM_DEFAULT,
            definition = AlarmDefinition.Custom("Test Arihna", LocalTime.NOON),
        )
        val occurrence = AlarmOccurrence(
            alarmId = rule.alarmId,
            ruleRevision = rule.revision,
            triggerAt = Instant.parse("2026-09-05T10:00:00Z"),
            occurrenceToken = "token",
        )
        assertEquals(AlarmNotificationDeliveryResult.DELIVERED, delivery.deliver(rule, occurrence))
        assertEquals(listOf("post", "start"), events)
    }

    @Test
    fun ringtonePickerIsRestrictedToAlarmTonesAndNeverUsesSilentAsPlatformChoice() {
        val intent = AlarmRingtonePicker.createIntent("content://alarm/current")
        assertEquals(RingtoneManager.ACTION_RINGTONE_PICKER, intent.action)
        assertEquals(RingtoneManager.TYPE_ALARM, intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1))
        assertTrue(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false))
        assertEquals(false, intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true))
    }
}
''')

write("app/src/androidTest/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmDiagnosticAndroidTest.kt", r'''package com.archimedeprojects.arihna.feature.alarms.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDiagnosticAndroidTest {
    @Test
    fun api28SchedulesAndCancelsBothIsolatedOneMinuteDiagnosticKinds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scheduler = AlarmDiagnosticTestScheduler(
            context = context,
            notificationPermissionReader = AlarmNotificationPermissionReader { true },
            fullScreenAccess = AlarmFullScreenAccess(context),
        )
        try {
            assertEquals(
                AlarmDiagnosticScheduleResult.SCHEDULED,
                scheduler.scheduleOneMinute(AlarmDiagnosticKind.SYSTEM_ALARM),
            )
            scheduler.cancel()
            assertEquals(
                AlarmDiagnosticScheduleResult.SCHEDULED,
                scheduler.scheduleOneMinute(AlarmDiagnosticKind.ADHAN),
            )
        } finally {
            scheduler.cancel()
        }
    }
}
''')
