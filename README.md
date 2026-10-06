# OPENRNDR-based flocking simulations

I wanted to learn more about [OPENRNDR](https://openrndr.org), a Kotlin framework for creative coding,
and to visualise Craig Reynolds's [boids](https://www.red3d.com/cwr/boids/) model of flocking. In Reynolds's model
each boid follows three local steering rules: keep apart from close neighbours (separation), match their heading
(alignment) and move towards their centre (cohesion). No boid knows the shape of the flock, yet a flock forms.

## Boids

The boids code began as a port of Daniel Shiffman's [flocking example](https://processing.org/examples/flocking.html)
in Processing. From there, the project moved in two directions, both aimed at simulating and drawing many more boids.

**Finding neighbours.** In the original, every boid checks every other boid on every frame, so the work grows with the
square of the flock size. This project builds a [k-d tree](https://en.wikipedia.org/wiki/K-d_tree) of boid positions
each frame and asks it only for the boids within the neighbour radius. The tree keeps the simulation responsive with a
large flock.

**Drawing.** OPENRNDR's way of drawing the shapes proved slow at this scale. The project instead writes every boid's
triangle into a single vertex buffer and draws the whole flock with one batched call per frame.

|                                     |                                     |
|:-----------------------------------:|:-----------------------------------:|
| ![Boids-1](screenshots/boids-1.png) | ![Boids-2](screenshots/boids-2.png) |

## Voronoi boids

The second program experiments with visualising the flock's movement through Voronoi diagrams. On every frame it
divides the window into cells, one per boid, each covering the area closer to that boid than to any other. It colours
each cell by its boid's speed. As the flock moves, the cells shift and reshape with it.

|                                                   |                                                   |
|:-------------------------------------------------:|:-------------------------------------------------:|
| ![voronoiboids-1](screenshots/voronoiboids-1.png) | ![voronoiboids-2](screenshots/voronoiboids-2.png) |

## Side by Side

The third program runs a single flock and shows it both ways at once: the boids on the left half of the window and
their Voronoi cells on the right. Each boid sits at the same spot in both halves, so the two views can be compared
directly. Where the boids crowd together their cells are small, and in the gaps between groups the cells grow large.

![sidebyside-1](screenshots/sidebyside-1.png)


... and a sample video:

https://github.com/user-attachments/assets/80cd25c9-5cbc-447e-bcc7-c0c424be5764




## Running

```bash
./gradlew run                                                           # boids
./gradlew run -Popenrndr.application=voronoiboids.VoronoiFlockingKt     # Voronoi boids
./gradlew run -Popenrndr.application=sidebyside.SideBySideKt            # side by side
```

Click anywhere in the window to add more boids at the cursor. In the side-by-side program they appear in both halves.
