package com.furybook.android.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.furybook.ui.theme.DublAccent
import com.furybook.ui.theme.DublBorder
import com.furybook.ui.theme.DublSurfaceRaised
import com.furybook.ui.theme.DublText

object DublSwitchTokens {
    val checkedThumb: Color = DublText
    val checkedTrack: Color = DublAccent
    val uncheckedThumb: Color = DublText
    val uncheckedTrack: Color = DublSurfaceRaised
    val uncheckedBorder: Color = DublBorder
}

@Composable
fun DublSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = DublSwitchTokens.checkedThumb,
            checkedTrackColor = DublSwitchTokens.checkedTrack,
            checkedBorderColor = DublSwitchTokens.checkedTrack,
            uncheckedThumbColor = DublSwitchTokens.uncheckedThumb,
            uncheckedTrackColor = DublSwitchTokens.uncheckedTrack,
            uncheckedBorderColor = DublSwitchTokens.uncheckedBorder,
            disabledCheckedThumbColor = DublSwitchTokens.checkedThumb.copy(alpha = 0.58f),
            disabledCheckedTrackColor = DublSwitchTokens.checkedTrack.copy(alpha = 0.38f),
            disabledCheckedBorderColor = DublSwitchTokens.checkedTrack.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = DublSwitchTokens.uncheckedThumb.copy(alpha = 0.58f),
            disabledUncheckedTrackColor = DublSwitchTokens.uncheckedTrack.copy(alpha = 0.52f),
            disabledUncheckedBorderColor = DublSwitchTokens.uncheckedBorder.copy(alpha = 0.52f),
        ),
    )
}
