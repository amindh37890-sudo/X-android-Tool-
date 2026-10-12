package com.example

import com.example.data.model.BatchActionType
import com.example.data.model.BatchProgressState
import com.example.data.model.DeepCategory
import com.example.data.model.DeepFeaturesRegistry
import com.example.data.model.PrivilegeLevel
import com.example.data.model.UltraPrivilegeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPrivilegeLevelsHierarchy() {
        val root = PrivilegeLevel.ROOT
        val adb = PrivilegeLevel.WIRELESS_ADB
        val shell = PrivilegeLevel.APP_SHELL
        val restricted = PrivilegeLevel.RESTRICTED

        assertEquals("ROOT (SUPERUSER)", root.title)
        assertEquals("WIRELESS DEBUGGING", adb.title)
        assertTrue(root.ordinal < adb.ordinal)
        assertTrue(adb.ordinal < shell.ordinal)
        assertTrue(shell.ordinal < restricted.ordinal)
    }

    @Test
    fun testBatchActionTypes() {
        val actions = BatchActionType.values()
        assertTrue(actions.contains(BatchActionType.SUPERCHARGE_MAX_PRIVILEGES))
        assertTrue(actions.contains(BatchActionType.AUTHORIZE_NEXUS))
        assertTrue(actions.contains(BatchActionType.EXEMPT_BATTERY_SAVER))
        assertTrue(actions.contains(BatchActionType.SET_STANDBY_ACTIVE))
        assertTrue(actions.contains(BatchActionType.FREEZE_APPS))
        assertTrue(actions.contains(BatchActionType.DEFROST_APPS))
        assertTrue(actions.contains(BatchActionType.FORCE_STOP))
        assertTrue(actions.contains(BatchActionType.CLEAR_DATA))
    }

    @Test
    fun testBatchProgressCalculation() {
        val progress = BatchProgressState(
            isExecuting = true,
            totalCount = 10,
            completedCount = 5,
            successfulCount = 4,
            failedCount = 1
        )
        assertEquals(0.5f, progress.progressPercent)
        assertFalse(progress.isCompleted)
    }

    @Test
    fun testUltraPrivilegesDefined() {
        val ultraList = UltraPrivilegeRegistry.privileges
        assertTrue(ultraList.isNotEmpty())
        assertTrue(ultraList.any { it.id == "silent_installer" })
        assertTrue(ultraList.any { it.id == "media_projection" })
        assertTrue(ultraList.any { it.id == "raw_storage_bypass" })
        assertTrue(ultraList.any { it.id == "input_injection" })
        assertTrue(ultraList.any { it.id == "network_traffic_control" })
        assertTrue(ultraList.any { it.id == "thermal_doze_bypass" })
    }

    @Test
    fun testExactly100DeepDangerousFeaturesPresent() {
        val features = DeepFeaturesRegistry.allFeatures
        assertEquals("Must contain exactly 100 deep dangerous features", 100, features.size)

        // Verify sequential numbering 1 to 100
        val numbers = features.map { it.number }.sorted()
        assertEquals(1, numbers.first())
        assertEquals(100, numbers.last())
        assertEquals(100, numbers.distinct().size)

        // Verify all 10 categories are represented
        val categories = features.map { it.category }.distinct()
        assertEquals(10, categories.size)
        assertEquals(10, DeepCategory.values().size)

        // Verify each feature has comprehensive warning in Arabic & English, commands, and hazards
        features.forEach { feature ->
            assertNotNull(feature.id)
            assertTrue("Feature #${feature.number} must have Arabic title", feature.titleAr.isNotBlank())
            assertTrue("Feature #${feature.number} must have English title", feature.titleEn.isNotBlank())
            assertTrue("Feature #${feature.number} must have explicit Arabic warning", feature.warningAr.isNotBlank())
            assertTrue("Feature #${feature.number} must have explicit English warning", feature.warningEn.isNotBlank())
            assertTrue("Feature #${feature.number} must have execution command", feature.command.isNotBlank())
            assertTrue("Feature #${feature.number} must have revert command", feature.commandRevert.isNotBlank())
            assertTrue("Feature #${feature.number} must have hazards listed", feature.potentialHazards.isNotEmpty())
            assertTrue("Feature #${feature.number} risk score must be high (>=70)", feature.riskScore >= 70)
        }
    }
}
