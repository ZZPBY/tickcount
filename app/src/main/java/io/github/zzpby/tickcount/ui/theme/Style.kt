package io.github.zzpby.tickcount.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The shapes the app is cut from, in one place.
 *
 * Generous radii and a hairline outline rather than a shadow: a card that is white on a warm
 * page already reads as raised, and a shadow on top of that reads as a decoration. The outline
 * is one pixel and the same colour everywhere, which is what makes a screen of cards look like
 * one system instead of several.
 */
val CardShape = RoundedCornerShape(26.dp)
val PanelShape = RoundedCornerShape(30.dp)
val FieldShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(50)

/** The horizontal margin every screen shares, so their contents line up down the page. */
val Gutter = 20.dp

/** The gap between two cards in a list. */
val CardGap = 10.dp

/**
 * The one-pixel outline a card carries.
 *
 * A function rather than a constant so it follows the scheme, and so a card can pass a
 * different colour when it wants to — the accent for something selected, say.
 */
@Composable
fun Modifier.hairline(
    shape: androidx.compose.ui.graphics.Shape = CardShape,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outlineVariant,
): Modifier = border(width = 1.dp, color = color, shape = shape)

/**
 * The wash behind something that is highlighted rather than merely present — the batch
 * section in the editor, the picker's own preview.
 *
 * The accent's container at a partial alpha, so it reads as the accent's own tint on both a
 * light and a dark page rather than as a second colour.
 */
@Composable
fun tintWash() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
