package com.goldex.companion.desktop.ui.controls

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog as NativeDialog
import androidx.compose.material3.AlertDialog as NativeAlertDialog
import androidx.compose.material3.ModalBottomSheet as NativeModalBottomSheet
import com.goldex.companion.ui.theme.LuxuryMotion

internal val LocalDesktopDialogInputEnabled = staticCompositionLocalOf { true }
private class DialogMotion(val opacity: State<Float>, val scale: State<Float>, val offset: State<Float>, val active: Boolean)
private val LocalDialogMotion = staticCompositionLocalOf<DialogMotion?> { null }

@Composable internal fun DesktopDialogHost(visible: Boolean, modal: Boolean = false, content: @Composable () -> Unit) =
    DesktopDialogHost(if (visible) Unit else null, modal) { content() }

/** Retains the last rendered snapshot until exit completes; never delays a domain save or cancellation. */
@Composable internal fun <T : Any> DesktopDialogHost(
    value: T?, modal: Boolean = false, contentKey: (T) -> Any? = { Unit }, content: @Composable (T) -> Unit
) {
    var retained by remember { mutableStateOf<T?>(null) }
    val visible = value != null && LocalDesktopDialogInputEnabled.current
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = visible
    val transition = updateTransition(visibility, label = "desktop-dialog-lifetime")
    val reduced = LocalDesktopReduceMotion.current
    val enter = if (modal) LuxuryMotion.DURATION_MODAL_ENTER else LuxuryMotion.DURATION_DIALOG_ENTER
    val exit = if (modal) LuxuryMotion.DURATION_MODAL_EXIT else LuxuryMotion.DURATION_DIALOG_EXIT
    val opacity = transition.animateFloat(transitionSpec = {
        tween(if (reduced) 0 else if (targetState) enter else exit,
            easing = if (targetState) LuxuryMotion.StandardEasing else LuxuryMotion.AccelerationEasing)
    }, label = "dialog-opacity") { if (it) 1f else 0f }
    val scale = transition.animateFloat(transitionSpec = {
        tween(if (reduced) 0 else if (targetState) enter else exit, easing = LuxuryMotion.EmphasizedEasing)
    }, label = "dialog-scale") { if (it || modal) 1f else .95f }
    val offset = transition.animateFloat(transitionSpec = {
        tween(if (reduced) 0 else if (targetState) enter else exit, easing = LuxuryMotion.StandardEasing)
    }, label = "dialog-offset") { if (!it && modal) 24f else 0f }
    val mounted = visible || visibility.currentState || !visibility.isIdle
    val displayed = value ?: retained
    SideEffect { if (value != null) retained = value else if (!mounted) retained = null }
    if (mounted && displayed != null) {
        val motion = remember(opacity, scale, offset, visible) { DialogMotion(opacity, scale, offset, visible) }
        CompositionLocalProvider(LocalDialogMotion provides motion, LocalDesktopDialogInputEnabled provides visible) {
            key(contentKey(displayed)) { content(displayed) }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable private fun Modifier.dialogMotion(): Modifier {
    val motion = LocalDialogMotion.current ?: return this
    return graphicsLayer {
        alpha = motion.opacity.value
        scaleX = motion.scale.value; scaleY = motion.scale.value
        translationY = motion.offset.value * density
    }.onPreviewKeyEvent { !motion.active }
        .then(if (motion.active) Modifier else Modifier.semantics { invisibleToUser() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            })
}

@Composable internal fun DesktopDialog(
    onDismissRequest: () -> Unit, properties: DialogProperties = DialogProperties(), content: @Composable () -> Unit
) {
    val active = LocalDesktopDialogInputEnabled.current
    NativeDialog({ if (active) onDismissRequest() }, properties) {
        Box(Modifier.dialogMotion()) { content() }
    }
}

@Composable internal fun DesktopAlertDialog(
    onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null, icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null, text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape, containerColor: Color = AlertDialogDefaults.containerColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation, properties: DialogProperties = DialogProperties()
) {
    val active = LocalDesktopDialogInputEnabled.current
    NativeAlertDialog(onDismissRequest = { if (active) onDismissRequest() }, confirmButton = confirmButton,
        modifier = modifier.dialogMotion(), dismissButton = dismissButton, icon = icon, title = title, text = text,
        shape = shape, containerColor = containerColor, tonalElevation = tonalElevation, properties = properties)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun DesktopModalBottomSheet(
    onDismissRequest: () -> Unit, sheetState: SheetState, modifier: Modifier = Modifier,
    shape: Shape = BottomSheetDefaults.ExpandedShape, containerColor: Color = BottomSheetDefaults.ContainerColor,
    scrimColor: Color = BottomSheetDefaults.ScrimColor, dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    val active = LocalDesktopDialogInputEnabled.current
    val opacity = LocalDialogMotion.current?.opacity?.value ?: 1f
    NativeModalBottomSheet(onDismissRequest = { if (active) onDismissRequest() }, sheetState = sheetState,
        modifier = modifier.dialogMotion(), shape = shape, containerColor = containerColor,
        scrimColor = scrimColor.copy(alpha = scrimColor.alpha * opacity), dragHandle = dragHandle, content = content)
}
