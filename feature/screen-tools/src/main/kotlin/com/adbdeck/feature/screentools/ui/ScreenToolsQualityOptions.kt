package com.adbdeck.feature.screentools.ui

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_bitrate_format
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenrecord_balanced_540
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenrecord_eco_360
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenrecord_high_1080
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenrecord_low_480
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenrecord_medium_720
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenrecord_ultra_native
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenshot_jpeg_best
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenshot_jpeg_high
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenshot_jpeg_low
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenshot_jpeg_medium
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenshot_jpeg_tiny
import adbdeck.feature.screen_tools.generated.resources.screen_tools_quality_screenshot_png_lossless
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.feature.screentools.ScreenrecordQualityPreset
import com.adbdeck.feature.screentools.ScreenshotQualityPreset
import org.jetbrains.compose.resources.stringResource

/**
 * Возвращает локализованные опции качества screenshot.
 */
@Composable
internal fun rememberScreenshotQualityOptions(): List<AdbDropdownOption<ScreenshotQualityPreset>> {
    val labels = linkedMapOf(
        ScreenshotQualityPreset.LOSSLESS_PNG to stringResource(Res.string.screen_tools_quality_screenshot_png_lossless),
        ScreenshotQualityPreset.JPEG_BEST to stringResource(Res.string.screen_tools_quality_screenshot_jpeg_best),
        ScreenshotQualityPreset.JPEG_HIGH to stringResource(Res.string.screen_tools_quality_screenshot_jpeg_high),
        ScreenshotQualityPreset.JPEG_MEDIUM to stringResource(Res.string.screen_tools_quality_screenshot_jpeg_medium),
        ScreenshotQualityPreset.JPEG_LOW to stringResource(Res.string.screen_tools_quality_screenshot_jpeg_low),
        ScreenshotQualityPreset.JPEG_TINY to stringResource(Res.string.screen_tools_quality_screenshot_jpeg_tiny),
    )
    return remember(labels) {
        labels.map { (preset, label) ->
            AdbDropdownOption(
                value = preset,
                label = label,
            )
        }
    }
}

/**
 * Возвращает локализованные опции качества screenrecord с подписью bitrate.
 */
@Composable
internal fun rememberScreenrecordQualityOptions(): List<AdbDropdownOption<ScreenrecordQualityPreset>> {
    val options = ScreenrecordQualityPreset.entries.map { preset ->
        val qualityTitle = when (preset) {
            ScreenrecordQualityPreset.ULTRA_NATIVE -> stringResource(Res.string.screen_tools_quality_screenrecord_ultra_native)
            ScreenrecordQualityPreset.HIGH_1080P -> stringResource(Res.string.screen_tools_quality_screenrecord_high_1080)
            ScreenrecordQualityPreset.MEDIUM_720P -> stringResource(Res.string.screen_tools_quality_screenrecord_medium_720)
            ScreenrecordQualityPreset.BALANCED_540P -> stringResource(Res.string.screen_tools_quality_screenrecord_balanced_540)
            ScreenrecordQualityPreset.LOW_480P -> stringResource(Res.string.screen_tools_quality_screenrecord_low_480)
            ScreenrecordQualityPreset.ECO_360P -> stringResource(Res.string.screen_tools_quality_screenrecord_eco_360)
        }
        val label = stringResource(
            Res.string.screen_tools_quality_bitrate_format,
            qualityTitle,
            preset.bitRateMbps,
        )
        AdbDropdownOption(
            value = preset,
            label = label,
        )
    }
    return remember(options) { options }
}
