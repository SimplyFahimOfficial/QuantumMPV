/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.quantummpv.app.ui.liquidglass.LocalLiquidGlassBackdrop
import com.quantummpv.app.ui.liquidglass.liquidGlassSurface

/**
 * Surfaces for browser, navigation, dialogs, player controls and sheets.
 *
 * Every surface is a solid Material surface. The one exception is the floating dock (pill, search button
 * and overflow menu): while experimental Liquid Glass is on, it draws real backdrop glass over the screen
 * content recorded by [LocalLiquidGlassBackdrop].
 */
object GlassComponents {
    @Composable
    fun GlassBottomSheetBackground(
        modifier: Modifier = Modifier,
        shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        classicColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        content: @Composable () -> Unit,
    ) {
        GlassContainer(
            modifier = modifier,
            shape = shape,
            classicColor = classicColor,
            content = content,
        )
    }

    @Composable
    fun GlassDialogBackground(
        modifier: Modifier = Modifier,
        shape: Shape = MaterialTheme.shapes.extraLarge,
        classicColor: Color = MaterialTheme.colorScheme.surface,
        classicTonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
        content: @Composable () -> Unit,
    ) {
        GlassContainer(
            modifier = modifier,
            shape = shape,
            classicColor = classicColor,
            classicTonalElevation = classicTonalElevation,
            content = content,
        )
    }

    @Composable
    fun GlassMiniPlayerSurface(
        modifier: Modifier = Modifier,
        shape: Shape = MaterialTheme.shapes.large,
        content: @Composable () -> Unit,
    ) {
        GlassContainer(modifier = modifier, shape = shape, content = content)
    }

    @Composable
    fun GlassMediaCardSurface(
        modifier: Modifier = Modifier,
        shape: Shape = MaterialTheme.shapes.medium,
        content: @Composable () -> Unit,
    ) {
        GlassContainer(modifier = modifier, shape = shape, content = content)
    }

    @Composable
    fun GlassFloatingMenuSurface(
        modifier: Modifier = Modifier,
        shape: Shape = MaterialTheme.shapes.extraLarge,
        forceGlass: Boolean = false,
        // Callers that animate the panel pass 0 and cast the shadow from their own animated layer.
        classicShadowElevation: androidx.compose.ui.unit.Dp = 12.dp,
        content: @Composable () -> Unit,
    ) {
        // The menu opens and closes frequently, so it uses the lighter navigation optics and an
        // always-visible edge for contrast. Classic is the same solid, bordered container as the dock.
        GlassContainer(
            modifier = modifier,
            shape = shape,
            opacityBoost = 0.16f,
            forceGlass = forceGlass,
            lightweightNav = true,
            alwaysBorder = true,
            classicColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            classicTonalElevation = 6.dp,
            classicShadowElevation = classicShadowElevation,
            classicBorder = navigationDockBorder(),
            content = content,
        )
    }

    @Composable
    fun GlassNavigationPill(
        modifier: Modifier = Modifier,
        isSelected: Boolean = false,
        forceGlass: Boolean = false,
        shape: Shape = MaterialTheme.shapes.medium,
        content: @Composable () -> Unit,
    ) {
        // The pill animates constantly, so it uses the lighter navigation optics.
        GlassContainer(
            modifier = modifier,
            shape = shape,
            selected = isSelected,
            forceGlass = forceGlass,
            lightweightNav = true,
            classicColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            classicTonalElevation = 6.dp,
            classicShadowElevation = 8.dp,
            classicBorder = navigationDockBorder(),
            content = content,
        )
    }

    /** The hairline edge shared by every Classic dock surface: pill, search circle and menu. */
    @Composable
    fun navigationDockBorder(): BorderStroke =
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f))
}

@Composable
private fun GlassContainer(
    modifier: Modifier,
    shape: Shape,
    opacityBoost: Float = 0f,
    selected: Boolean = false,
    forceGlass: Boolean = false,
    lightweightNav: Boolean = false,
    alwaysBorder: Boolean = false,
    classicColor: Color = MaterialTheme.colorScheme.surface,
    classicTonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    classicShadowElevation: androidx.compose.ui.unit.Dp = 0.dp,
    classicBorder: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Only the dock asks for glass, and only surfaces above a captured screen layer can have a backdrop.
    val liquidBackdrop = LocalLiquidGlassBackdrop.current
    if (liquidBackdrop != null && forceGlass) {
        LiquidGlassContainer(
            backdrop = liquidBackdrop,
            modifier = modifier,
            shape = shape,
            tintColor = (if (selected) colors.primaryContainer else colors.surface).copy(alpha = 0.34f + opacityBoost),
            lightweightNav = lightweightNav,
            alwaysBorder = alwaysBorder,
            content = content,
        )
        return
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (selected) colors.primaryContainer else classicColor,
        tonalElevation = classicTonalElevation,
        shadowElevation = classicShadowElevation,
        border = classicBorder,
        content = content,
    )
}

/** The Kyant renderer: real backdrop blur plus lens refraction, tuned on the Liquid Glass settings page. */
@Composable
private fun LiquidGlassContainer(
    backdrop: Backdrop,
    modifier: Modifier,
    shape: Shape,
    tintColor: Color,
    lightweightNav: Boolean,
    alwaysBorder: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .liquidGlassSurface(backdrop, shape, tintColor, lightweight = lightweightNav)
                .then(
                    if (alwaysBorder) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f), shape)
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
