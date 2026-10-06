package boids

import boids.KDTree.KDNode

/** A KDNode node that knows its [boid]; the [KDNode.point] is the boid position. */
private class BoidNode(val boid: Boid) : KDNode(boid.position)


/**
 * The collection of all boids; each frame a [KDTree] of their positions
 * finds every boid's potential neighbours.
 */
class Flock {
    val boids = mutableListOf<Boid>()

    /**
     * Advances each boid by one frame inside a [width] x [height] area.
     * Boids are updated in place one after another, so later boids see the new
     * positions of earlier ones.
     * The tree holds the positions from the start of the frame; querying it
     * finds boids that were neighbours at the start of the frame, using their
     * current state.
     */
    fun run(width: Double, height: Double) {
        val tree = KDTree()
        for (boid in boids.shuffled()) {
            tree.add(BoidNode(boid))
        }

        // reused for every query; Boid.run does not keep the list
        val neighborBoids = ArrayList<Boid>()
        for (boid in boids) {
            neighborBoids.clear()
            tree.forEachNeighbor(boid.position, BoidConf.NEIGHBOR_RADIUS) { neighborBoids += (it as BoidNode).boid }

            boid.run(neighborBoids, width, height)
        }
    }

    fun add(boid: Boid) {
        boids += boid
    }
}
