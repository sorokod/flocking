package boids

import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import kotlin.math.roundToInt

private val SLOW_COLOUR = ColorRGBa.fromHex(0xFF2E5C).toHSVa() // "red"
private val FAST_COLOUR = ColorRGBa.fromHex(0x2EC5FF).toHSVa() // "blue"

private const val COLOR_STEPS = 256

/**
 * Exact (uncached) colour for [speedFraction] of top speed: [SLOW_COLOUR] at 0, [FAST_COLOUR] at 1.
 * The blend interpolates in HSV, so the hue passes through magenta and violet and stays saturated and bright.
 * An RGB blend would give a dull mauve at the midpoint.
 */
private fun blendSpeedColor(speedFraction: Double): ColorRGBa =
    mix(SLOW_COLOUR, FAST_COLOUR, speedFraction).toRGBa().toLinear()

/** [blendSpeedColor] precomputed at [COLOR_STEPS] evenly spaced fractions from 0 to 1, both ends included. */
private val SPEED_COLORS: List<ColorRGBa> = List(COLOR_STEPS) { blendSpeedColor(it / (COLOR_STEPS - 1.0)) }

/**
 * Fill colour for a boid at [speedFraction] of its top speed, from [SLOW_COLOUR] to [FAST_COLOUR],
 * in linear RGB. Rounded to the nearest of [COLOR_STEPS] precomputed colours, so it is cheap enough
 * to call per boid per frame.
 *
 * [speedFraction] must lie in 0..1 (see [Boid.speedFraction]). It is not clamped: NaN or a value
 * well outside that range throws.
 */
fun speedColor(speedFraction: Double): ColorRGBa =
    SPEED_COLORS[(speedFraction * (COLOR_STEPS - 1)).roundToInt()]
