package com.goldex.companion.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset

/**
 * Persian Sovereign Aurum Motion Tokens & Standardized Animation Specs.
 *
 * Provides a unified, luxury physical spring and easing model across
 * all modals, bottom sheets, full-screen sheets, and center dialogs.
 */
object LuxuryMotion {
    const val DURATION_MODAL_ENTER = 280
    const val DURATION_MODAL_EXIT = 220
    const val DURATION_DIALOG_ENTER = 240
    const val DURATION_DIALOG_EXIT = 180

    // Standard and Emphasized Easings
    val StandardEasing: Easing = FastOutSlowInEasing
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val AccelerationEasing: Easing = FastOutLinearInEasing

    // Physics-Based Springs
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SmoothSpring = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow
    )

    val IntOffsetSpring = spring<IntOffset>(
        dampingRatio = 0.84f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Modal / Bottom Sheet Transitions
    val ModalEnter: EnterTransition = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight },
        animationSpec = IntOffsetSpring
    ) + fadeIn(
        animationSpec = tween(durationMillis = 280, easing = StandardEasing)
    )

    val ModalExit: ExitTransition = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(durationMillis = 220, easing = AccelerationEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = 180, easing = LinearEasing)
    )

    // Center Dialog Transitions (Consistent, smooth subtle scale & fade)
    val DialogEnter: EnterTransition = scaleIn(
        initialScale = 0.93f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = tween(durationMillis = 240, easing = StandardEasing)
    )

    val DialogExit: ExitTransition = scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(durationMillis = 180, easing = AccelerationEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = 150, easing = LinearEasing)
    )

    // Fullscreen Screen Push / Pop Transitions
    val ScreenPushEnter: EnterTransition = slideInHorizontally(
        animationSpec = IntOffsetSpring
    ) { fullWidth -> -fullWidth / 3 } + fadeIn(
        animationSpec = tween(durationMillis = 260, easing = StandardEasing)
    )

    val ScreenPopExit: ExitTransition = slideOutHorizontally(
        animationSpec = tween(durationMillis = 220, easing = AccelerationEasing)
    ) { fullWidth -> -fullWidth / 3 } + fadeOut(
        animationSpec = tween(durationMillis = 180)
    )
}
