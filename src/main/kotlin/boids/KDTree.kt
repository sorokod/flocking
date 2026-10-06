package boids

import org.openrndr.math.Vector2

/**
 * Simple implementation of a 2-D tree. [KDNode] is a tree node with left and right pointers
 * and the point value of [Vector2].
 * Levels alternate between splitting on x (even depths) and y (odd depths).
 *
 * See https://en.wikipedia.org/wiki/K-d_tree
 */
class KDTree() {

    open class KDNode(val point: Vector2) {
        var left: KDNode? = null
            private set
        var right: KDNode? = null
            private set

        internal fun add(node: KDNode, depth: Int) {
            if (node.point.axis(depth) < point.axis(depth)) {
                left?.add(node, depth + 1) ?: run { left = node }
            } else {
                right?.add(node, depth + 1) ?: run { right = node }
            }
        }
    }

    var root: KDNode? = null
        private set

    constructor(root: KDNode) : this() {
        this.root = root
    }

    constructor(points: List<Vector2>) : this() {
        points.forEach(::add)
    }

    fun add(node: KDNode) {
        root?.add(node, depth = 0) ?: run { root = node }
    }

    fun add(point: Vector2) = add(KDNode(point))

    /**
     * Calls [action] on every KNode such that d([KDNode.point], [target]) <= [radius].
     * Throws [IllegalArgumentException] if [radius] is negative.
     */
    fun forEachNeighbor(target: Vector2, radius: Double, action: (KDNode) -> Unit) {
        require(radius >= 0.0) {"radius must be positive, was $radius"}

        collect(root, 0, target, radius, radius * radius, action)
    }

    private fun collect(
        node: KDNode?, depth: Int, target: Vector2, radius: Double, radiusSquared: Double, action: (KDNode) -> Unit,
    ) {
        if (node == null) return
        if (node.point.squaredDistanceTo(target) <= radiusSquared) action(node)

        // the left subtree holds points below the split, the right subtree points at or above it;
        // skip a subtree when the circle around the target lies entirely on the other side
        val split = node.point.axis(depth)
        if (target.axis(depth) - radius < split) collect(node.left, depth + 1, target, radius, radiusSquared, action)
        if (target.axis(depth) + radius >= split) collect(node.right, depth + 1, target, radius, radiusSquared, action)
    }
}

/** The value to split on at [depth]: x at even depths, y at odd depths. */
private fun Vector2.axis(depth: Int): Double = this[depth % 2]
