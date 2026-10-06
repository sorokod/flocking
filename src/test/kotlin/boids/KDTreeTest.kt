package boids

import boids.KDTree.KDNode
import org.junit.Assert.*
import org.junit.Test
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector2.Companion.ONE
import kotlin.random.Random

class KDTreeTest {


    /**
     * Inserted in order, these points form this tree. A point goes left when its coordinate
     * on the level's axis is below the node's, otherwise right.
     *
     * ```
     *  70 +-----------------------------------+-----------------------------------+
     *     |                                   |                                   |
     *     |      (10,60)                      |                                   |
     *  60 +---------o---------------+---------+                                   |
     *     |                         |         |                                   |
     *     |                         |         | (50,50)                           |
     *  50 |                         |         o                                   |
     *     |                         |         |                                   |
     *     |                         |         |                    (80,40)        |
     *  40 |                         |         +---------+-------------o-----------+
     *     |                         |         |         |                         |
     *  38 |                 (48,38) o         |         o (51,38)                 |
     *     |                         |         |         |                         |
     *     |                         |         |         |                         |
     *     |                         |         |         |                         |
     *   0 +-------------------------+---------+---------+-------------------------+
     *     0        10              48        50        51            80          100
     * ```
     *
     * ```
     * depth 0 (split on x)                  (50, 50)
     *                                      /        \
     *                                 x < 50        x >= 50
     *                                   /                \
     * depth 1 (split on y)        (10, 60)              (80, 40)
     *                             /      \              /      \
     *                        y < 60    y >= 60     y < 40    y >= 40
     *                           /          \          /          \
     * depth 2 (split on x)  (48, 38)       null   (51, 38)       null
     * ```
     */
    private val testPoints = listOf(
        V(50, 50),
        V(80, 40),
        V(10, 60),
        V(51, 38),
        V(48, 38),
    )

    @Test
    fun `empty tree has no root`() {
        assertNull(KDTree().root)
    }

    @Test
    fun `empty list builds an empty tree`() {
        assertNull(KDTree(emptyList()).root)
    }

    @Test
    fun `constructor with root uses that node`() {
        val node = KDNode(ONE)
        val tree = KDTree(node)

        assertSame(node, tree.root)
    }

    @Test
    fun `levels alternate between splitting on x and y`() {
        val root = KDTree(testPoints).root!!

        assertEquals(V(50, 50), root.point)
        // depth 0 splits on x
        assertEquals(V(10, 60), root.left!!.point)
        assertEquals(V(80, 40), root.right!!.point)
        // depth 1 splits on y
        assertEquals(V(48, 38), root.left!!.left!!.point)
        assertNull(root.left!!.right)
        assertEquals(V(51, 38), root.right!!.left!!.point)
        assertNull(root.right!!.right)
    }

    @Test
    fun `equal coordinate goes to the right subtree`() {
        val tree = KDTree(
            listOf(
                V(5, 5),
                V(5, 0)
            )
        )

        with(tree.root!!) {
            assertEquals(point, V(5, 5))
            assertEquals(V(5, 0), right!!.point)
            assertNull(left)

        }
    }

    @Test
    fun `empty tree has no neighbors`() {
        assertTrue(KDTree().neighborsOf(ONE, 1.0).isEmpty())
    }

    @Test
    fun `negative radius throws IllegalArgumentException`() {
        val tree = KDTree(testPoints)

        val exception = assertThrows(IllegalArgumentException::class.java) { tree.neighborsOf(ONE, -1.0) }

        assertEquals("radius must be positive, was -1.0", exception.message)
    }

    @Test
    fun `finds the neighbors within a radius`() {
        val tree = KDTree(testPoints)
        val target = V(40, 40)

        // distances from target:
        // (48, 38) = 8.2,
        // (51, 38) = 11.1,
        // (50, 50) = 14.1
        // (10, 60) = 36.0
        // (80, 40) = 40.0

        assertEquals(
            listOf(V(48, 38)),
            sortedPoints(tree.neighborsOf(target, 10.0))
        )
        assertEquals(
            listOf(V(48, 38), V(51, 38)),
            sortedPoints(tree.neighborsOf(target, 12.0)),
        )
        assertEquals(emptyList<Vector2>(), sortedPoints(tree.neighborsOf(target, 8.0)))
    }

    @Test
    fun `a point exactly at the radius is a neighbor`() {
        val neighbor = Polar(0.0, 5.0).cartesian
        val tree = KDTree(listOf(neighbor))

        assertEquals(listOf(neighbor), sortedPoints(tree.neighborsOf(V(0, 0), 5.0)))
        assertEquals(listOf(neighbor), sortedPoints(tree.neighborsOf(V(5, 5), 5.0)))
    }

    @Test
    fun `zero radius finds every copy of the target`() {
        val tree = KDTree(listOf(V(1, 1), V(2, 2), V(1, 1)))

        assertEquals(
            listOf(V(1, 1), V(1, 1)),
            sortedPoints(tree.neighborsOf(V(1, 1), 0.0))
        )
    }

    @Test
    fun `neighbors match brute force on random points`() {
        val random = Random(0)
        val points = List(1000) { V(random.nextDouble(-500.0, 500.0), random.nextDouble(-500.0, 500.0)) }
        val tree = KDTree(points)

        repeat(500) {
            val target = V(random.nextDouble(-600.0, 600.0), random.nextDouble(-600.0, 600.0))
            assertNeighborsOf(points, tree, target, random.nextDouble(0.0, 150.0))
        }
    }

    @Test
    fun `neighbors match brute force on a grid with duplicates and ties`() {
        val random = Random(0)
        val points = List(300) { V(random.nextInt(10), random.nextInt(10)) }
        val tree = KDTree(points)

        for (x in -2..11)
            for (y in -2..11)
                for (radius in listOf(0.0, 1.0, 2.5, 5.0)) {
                    val target = V(x, y)
                    assertNeighborsOf(points, tree, target, radius)
                }
    }

    /**
     * Checks the neighbors of [target] in [tree] against a brute-force filter of [points] (boundary included).
     * The tree returns nodes in no particular order, so both are sorted before comparing.
     */
    private fun assertNeighborsOf(points: List<Vector2>, tree: KDTree, target: Vector2, radius: Double) {
        val expected: List<Vector2> =
            points.filter { it.squaredDistanceTo(target) <= radius * radius }.sortedWith(byXThenY)
        val actual: List<Vector2> =
            sortedPoints(tree.neighborsOf(target, radius))

        assertEquals("neighbors of $target within $radius", expected, actual)
    }

    private val byXThenY: Comparator<Vector2> =
        compareBy({ it.x }, { it.y })

    private fun sortedPoints(kNodes: List<KDNode>): List<Vector2> =
        kNodes.map { it.point }.sortedWith(byXThenY)

    /** Collects what [KDTree.forEachNeighbor] visits, in visiting order. */
    private fun KDTree.neighborsOf(target: Vector2, radius: Double): List<KDNode> =
        buildList { forEachNeighbor(target, radius) { add(it) } }

    private fun <T : Number> V(x: T, y: T) = Vector2(x.toDouble(), y.toDouble())
}
