package boids

import org.openrndr.extra.noise.uniform
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector2.Companion.ZERO
import org.openrndr.math.mod

object BoidConf {
    const val R = 2.0
    const val R2 = R * 2 // the triangle width
    const val MAX_FORCE = 0.03 // maximum steering force
    const val MAX_SPEED = 2.0
    const val NEIGHBOR_RADIUS = 55.0 // alignment and cohesion radius
    const val DESIRED_SEPARATION = 25.0 // steers away from boids closer than this value in pixels
}

/**
 * @return this vector scaled down to length [max] if it is longer,
 * otherwise unchanged.
 */
private fun Vector2.scaleTo(max: Double): Vector2 =
    when (squaredLength > max * max) {
        true -> normalized * max
        false -> this
    }

/**
 * A single agent that steers itself using the boids within a radius around it.
 * Motion uses "integration": forces add to [acceleration], which adds to [velocity] once per frame.
 */
class Boid(var position: Vector2) {
    private var velocity: Vector2 = Polar(Double.uniform(0.0, 360.0), 1.0).cartesian
    private var acceleration: Vector2 = ZERO

    /** Current speed as a fraction of [maxSpeed]: 0 when at rest, 1 at the speed limit. */
    val speedFraction: Double
        get() = (velocity.length / BoidConf.MAX_SPEED).coerceIn(0.0, 1.0)

    /**
     * Advances this boid by one frame: computes the flocking forces from [boids],
     * moves, then wraps around the [width] x [height] area.
     * [boids] must include every boid within [neighborDist]; any others are ignored.
     */
    fun run(boids: List<Boid>, width: Double, height: Double) {
        flock(boids)
        update()
        borders(width, height)
    }

    /**
     * Applies the flocking rules by updating the acceleration for the current frame
     * - mass is taken as 1
     * Separation gets extra weight so boids keep their distance rather than collapsing into
     * a single clump.
     */
    private fun flock(boids: List<Boid>) {
        acceleration += (separate(boids) * 1.1)
        acceleration += (align(boids) * 1.0)
        acceleration += (cohesion(boids) * 1.0)
    }

    /**
     * Integrates acceleration into velocity and velocity into position,
     * then resets acceleration.
     */
    private fun update() {
        velocity = (velocity + acceleration).scaleTo(BoidConf.MAX_SPEED)
        position += velocity
        acceleration = ZERO
    }


    /** Wraps this boid to the opposite edge if it is off-screen */
    private fun borders(width: Double, height: Double) {
        position = position.mod(Vector2(width, height))
    }

    /**
     * Separation: steers away from boids closer than [BoidConf.desiredSeparation].
     * Each neighbor pushes with strength 1/distance, so the closest ones dominate.
     * The `d > 0` check skips this boid itself.
     */
    private fun separate(boids: List<Boid>): Vector2 {
        var steer: Vector2 = ZERO
        var count = 0
        for (boid in boids) {
            if (boid == this) continue

            val d = position.distanceTo(boid.position)
            if (d > 0 && d < BoidConf.DESIRED_SEPARATION) {
                // point away from other and weight by distance
                steer += (position - boid.position).normalized / d
                count++
            }
        }
        if (count > 0) steer /= count.toDouble()
        if (steer.length > 0) {
            steer = (steer.normalized * BoidConf.MAX_SPEED - velocity).scaleTo(BoidConf.MAX_FORCE)
        }
        return steer
    }

    /**
     * Alignment: steers towards the average heading of boids within 50 pixels.
     * Returns zero when there are no neighbours.
     */
    private fun align(boids: List<Boid>): Vector2 {
        var sum = ZERO
        var count = 0
        for (boid in boids) {
            if (boid == this) continue

            sum += boid.velocity
            count++
        }
        if (count == 0) return ZERO
        val desired = (sum / count.toDouble()).normalized * BoidConf.MAX_SPEED
        return (desired - velocity).scaleTo(BoidConf.MAX_FORCE)
    }

    /**
     * Cohesion: steers towards the centre of mass of boids within [BoidConf.NEIGHBOR_RADIUS] pixels.
     * Returns zero when there are no such neighbors.
     */
    private fun cohesion(boids: List<Boid>): Vector2 {

        /**
         * The force that turns the current velocity towards [target] at max speed,
         * i.e. `desired - velocity`, capped at [BoidConf.MAX_FORCE] so turns are gradual.
         */
        fun seek(target: Vector2): Vector2 {
            val desired = (target - position).normalized * BoidConf.MAX_SPEED
            return (desired - velocity).scaleTo(BoidConf.MAX_FORCE)
        }

        var sum = ZERO
        var count = 0
        for (boid in boids) {
            if (boid == this) continue

            if (position.squaredDistanceTo(boid.position) > 0) {
                sum += boid.position
                count++
            }
        }
        return when (count > 0) {
            true -> seek(sum / count.toDouble()) // the centre of mass of the local group
            false -> ZERO
        }
    }

    /**
     * The corners of the boid triangle in world coordinates, as `[nose, back − r·side, back + r·side]`:
     * an isosceles triangle with base 2r and height 4r, nose pointing along the velocity.
     *
     * ```
     *          nose          nodse: position + 2r·forward
     *           /\
     *          /  \
     *         / •  \         • position: 2r from the nose and 2r from the base
     *        /      \
     *       /________\       back: base centre = position − 2r·forward
     *   back − r·side   back + r·side
     * ```
     *
     * `position` is the midpoint of the height
     * Returning the vertices lets [FlockDrawer] draw all boids in batched calls.
     */
    fun vertices(): List<Vector2> {
        val forward = velocity.normalized
        val side = Vector2(-forward.y, forward.x) // rotate 90 deg.
        val back = position - forward * (BoidConf.R2)
        return listOf(
            position + (forward * (BoidConf.R2)),
            back - (side * BoidConf.R),
            back + (side * BoidConf.R)
        )
    }
}
