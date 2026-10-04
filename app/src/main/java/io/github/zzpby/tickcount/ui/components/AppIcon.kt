package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R

/**
 * The launcher icon, at whatever size the caller asks for.
 *
 * The two layers are composed here rather than drawn from `R.mipmap.ic_launcher`,
 * because that resource is an `<adaptive-icon>` — a description of a background
 * colour and a foreground layer, which neither `painterResource` nor a plain
 * `Canvas` can read. Composing it keeps the icon identical to the one the launcher
 * draws, so long as `mipmap-anydpi-v26/ic_launcher.xml` still names these two.
 *
 * Clipped to a rounded square because an adaptive icon relies on the launcher to
 * apply its mask, and nothing here does.
 */
@Composable
fun AppIcon(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(colorResource(R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
