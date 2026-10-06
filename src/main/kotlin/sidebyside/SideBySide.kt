package sidebyside

import boids.Boid
import boids.Flock
import boids.FlockDrawer
import org.openrndr.application
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.draw.isolated
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import voronoiboids.CellDrawer

/**
 * One flock shown two ways: the boids on the left half of the window (as in [boids]) and
 * their Voronoi cells on the right half (as in [voronoiboids]).
 * The flock lives in a half-window area, so each half shows the same boids at the same places.
 *
 * Click either half to add boids at that spot.
 */
fun main() = application {

    val initialBoidCount = 600

    configure {
        width = 1600
        height = 600
        windowResizable = true
    }

    program {
        val flock = Flock()

        val flockDrawer = FlockDrawer()
        val cellDrawer = CellDrawer()

        mouse.buttonDown.listen { event ->
            // map a click in either half to the flock's half-window coordinates
            val position = Vector2(event.position.x.mod(width / 2.0), event.position.y)
            repeat(10) {
                flock.add(Boid(position))
            }
        }

        repeat(initialBoidCount) {
            flock.add(
                Boid(Vector2.uniformRing(140.0, 180.0) + Vector2(width / 4.0, height / 2.0))
            )
        }

        var titleCount = -1

        extend(Screenshots())

        extend {

            if (flock.boids.size != titleCount) {
                titleCount = flock.boids.size
                window.title = "Boids | Voronoi Boids: $titleCount"
            }

            val half = Rectangle(0.0, 0.0, width / 2.0, height.toDouble())

            flock.run(half.width, half.height)

            drawer.clear(BLACK)

            // left: the boids; the clip (in window coordinates) keeps triangles at the edge out of the right half
            drawer.isolated {
                drawStyle.clip = half
                flockDrawer.draw(drawer, flock.boids)
            }

            // right: one cell per boid, in the same order as flock.boids
            val cells = flock.boids.map { it.position }
                .delaunayTriangulation()
                .voronoiDiagram(half)
                .cellPolygons()

            drawer.isolated {
                drawStyle.clip = half.movedBy(Vector2(half.width, 0.0))
                translate(half.width, 0.0)
                cellDrawer.draw(drawer, flock.boids, cells)

                fill = null
                stroke = BLACK
                strokeWeight = 1.0
                lineLoops(cells.filter { !it.empty }.map { c -> c.segments.map { it.start } })
            }
        }
    }
}
