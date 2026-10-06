package voronoiboids

import boids.Boid
import boids.Flock
import boids.speedColor
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
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * A variation on the flocking in [boids]: rather than drawing the boids, it draws the Voronoi cell
 * around each boid, filled with the boid's speed colour (see [speedColor]).
 * Cells are clipped to the window, so a cell jumps to the opposite edge when its boid wraps around.
 *
 * Click the mouse to add boids.
 */

/**
 * Fills Voronoi cells with one batched call per frame. Voronoi cells are convex, so each cell is
 * split into a fan of triangles around its first corner.
 */
class CellDrawer {
    private val format = vertexFormat {
        position(3)
        color(4)
    }
    private var triangles = vertexBuffer(format, 3 * 4096)

    /** Takes each triangle's fill from its vertex colours. */
    private val vertexColors = shadeStyle {
        fragmentTransform = "x_fill = va_color;"
    }

    /**
     * Fills `cells[i]` with the speed colour of `boids[i]`.
     * Empty cells, which belong to boids that coincide with another boid, are skipped.
     */
    fun draw(drawer: Drawer, boids: List<Boid>, cells: List<ShapeContour>) {
        // a closed polygon with n corners has n segments and splits into n - 2 triangles
        val vertexCount = cells.sumOf { 3 * maxOf(it.segments.size - 2, 0) }
        if (vertexCount == 0) return

        // Boids can be added at any time, so grow the buffer when the cells outgrow it.
        if (vertexCount > triangles.vertexCount) {
            triangles.destroy()
            triangles = vertexBuffer(format, maxOf(vertexCount, 2 * triangles.vertexCount))
        }

        triangles.put {
            for (i in cells.indices) {
                val color = speedColor(boids[i].speedFraction)
                val segments = cells[i].segments
                val first = segments[0].start.xy0
                for (j in 1 until segments.size - 1) {
                    write(first)
                    write(color)
                    write(segments[j].start.xy0)
                    write(color)
                    write(segments[j + 1].start.xy0)
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

    val initialBoidCount = 600

    configure {
        width = 800
        height = 600
        windowResizable = true
    }

    program {
        val flock = Flock()

        val cellDrawer = CellDrawer()

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

        var titleCount = -1

        extend(Screenshots())

        extend {

            if (flock.boids.size != titleCount) {
                titleCount = flock.boids.size
                window.title = "Voronoi Boids: $titleCount"
            }

            flock.run(width.toDouble(), height.toDouble())

            // one cell per boid, in the same order as flock.boids
            val cells = flock.boids.map { it.position }
                .delaunayTriangulation()
                .voronoiDiagram(drawer.bounds)
                .cellPolygons()

            drawer.clear(BLACK)
            cellDrawer.draw(drawer, flock.boids, cells)

            drawer.isolated {
                fill = null
                stroke = BLACK
                strokeWeight = 1.0
                lineLoops(cells.filter { !it.empty }.map { c -> c.segments.map { it.start } })
            }
        }
    }
}
