package boids

import org.openrndr.application
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2

/**
 * OPENRNDR port of the Processing flocking example.
 * See https://www.processing.org/examples/flocking.html implemented by Daniel Shiffman
 *
 * Click the mouse to add boids.
 */

/**
 * Draws every [Boid] with one batched call per frame: a vertex buffer of triangles, each filled
 * with a colour determined its boid's speed (see [speedColor]).
 */
class FlockDrawer {
    private val format = vertexFormat {
        position(3)
        color(4)
    }
    private var triangles = vertexBuffer(format, 3 * 512)

    /** Takes each triangle's fill from its vertex colours. */
    private val vertexColors = shadeStyle {
        fragmentTransform = "x_fill = va_color;"
    }

    fun draw(drawer: Drawer, boids: List<Boid>) {
        if (boids.isEmpty()) return

        // Boids can be added at any time, so grow the buffer when they outgrow it.
        val vertexCount = 3 * boids.size
        if (vertexCount > triangles.vertexCount) {
            triangles.destroy()
            triangles = vertexBuffer(format, maxOf(vertexCount, 2 * triangles.vertexCount))
        }

        triangles.put {
            for (boid in boids) {
                val color = speedColor(boid.speedFraction)
                for (vertex in boid.vertices()) {
                    write(vertex.xy0)
                    write(color)
                }
            }
        }

        drawer.isolated {
            shadeStyle = vertexColors
            vertexBuffer(triangles, DrawPrimitive.TRIANGLES, 0, vertexCount)
        }
    }
}


/**
 *
 */
fun main() = application {

    val initialBoidCount = 1000

    configure {
        width = 800
        height = 600
        windowResizable = true
    }

    program {
        val flock = Flock()

        val flockDrawer = FlockDrawer()

        mouse.buttonDown.listen { event ->
            repeat(10) {
                flock.add(Boid(event.position))
            }
        }

        repeat(initialBoidCount) {
            flock.add(
                Boid(Vector2.uniformRing(140.0, 180.0) + drawer.bounds.center)
            )
        }
        drawer.clear(BLACK)

        var titleCount = -1

        extend(Screenshots())

        extend {

            if (flock.boids.size != titleCount) {
                titleCount = flock.boids.size
                window.title = "Boids Flocking: $titleCount"
            }

            flock.run(width.toDouble(), height.toDouble())
            flockDrawer.draw(drawer, flock.boids)
        }
    }
}
