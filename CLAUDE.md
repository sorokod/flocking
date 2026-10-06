# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Project background, screenshots and run instructions for humans are in `README.md`; this file is for working on the
code.

## Commands

```bash
./gradlew build                                        # compile + test
./gradlew test --tests boids.KDTreeTest                # one test class
./gradlew test --tests 'boids.KDTreeTest.finds the neighbors within a radius'   # one test (backticked name)
```

- `-q` hides test counts. To confirm what ran, read `build/test-results/test/TEST-<class>.xml` (`tests=`, `failures=`).
  Add `--rerun-tasks` when an up-to-date `test` task would otherwise skip.
- `./gradlew run` (and `-Popenrndr.application=voronoiboids.VoronoiFlockingKt`) opens a GUI window and blocks until it
  is closed. Do not use it to verify changes; use `build` and tests, and ask the user to check visual behaviour.
- `openrndrRelease`, `openrndrSonatypeSnapshot` and `openrndrLocalSnapshot` query GitHub and rewrite
  `gradle/libs.versions.toml` and `gradle.properties`. Run them only when asked.
- No linter; `kotlin.code.style=official`.

## Constraints

- Tests go under `src/test/kotlin/`. No other test source set exists, so a test placed elsewhere is silently not
  compiled. Tests use JUnit 4 (`org.junit.Test`, `org.junit.Assert`).
- `settings.gradle.kts` reads the OPENRNDR and ORX versions by regex from the `openrndr = "..."` and `orx = "..."` lines
  in `gradle/libs.versions.toml` to load the `openrndr` and `orx` catalogs. Keep those lines in that plain form.
- JVM target and Kotlin API/language levels come from `libs.versions.toml` (`jvmTarget`, `kotlinApi`, `kotlinLanguage`),
  applied by the convention plugins in `buildSrc/src/main/kotlin/conventions/`.
- Both programs `extend(Screenshots())`, which writes PNGs into `screenshots/`. `README.md` embeds files from there, so
  do not delete or rename them without checking the README.

## Architecture

Two `main` functions share the simulation in package `boids`: `boids/Runner.kt` (`boids.RunnerKt`, the default
`applicationMainClass`) draws triangles; `voronoiboids/VoronoiFlocking.kt` draws each boid's Voronoi cell via ORX
`delaunayTriangulation().voronoiDiagram(bounds).cellPolygons()`. The cells come back in `flock.boids` order and
`CellDrawer` pairs them by index, so nothing may reorder `flock.boids` between `flock.run` and drawing.

Simulation behaviour that is not obvious from any one file:

- `Flock.run` rebuilds a `KDTree` every frame (shuffled insertion, private `BoidNode` subclass carrying the boid), then
  for each boid queries neighbours within `BoidConf.NEIGHBOR_RADIUS` and calls `Boid.run`.
- Boids update in place in sequence, so later boids see earlier boids' new state, while the tree holds start-of-frame
  positions.
- `Boid.align` and `Boid.cohesion` do no distance filtering; they trust the neighbour list. The query radius in `Flock`
  is therefore their radius, and changing it changes behaviour. Only `separate` filters again, by
  `DESIRED_SEPARATION`. Tuning constants live in `BoidConf` (`Boid.kt`).
- `KDTree` is unbalanced; equal coordinates go right and `forEachNeighbor` includes points exactly on the radius.
  `KDTreeTest` pins these rules and compares against brute force; keep both in step.
- `Boid`'s initial heading comes from unseeded `Double.uniform`, so tests of steering behaviour are not deterministic.
  `KDTree`, `Flock`, `Boid` and `speedColor` need no GL context; the drawers do and cannot be unit-tested here.

Rendering: `FlockDrawer` and `CellDrawer` write coloured triangles into a reused `vertexBuffer`, replacing it with a
larger one when boids outgrow it, and draw with one call per frame. A `shadeStyle` takes the fill from vertex colours
(`x_fill = va_color`). `speedColor` indexes a 256-entry table and does not clamp: a NaN `speedFraction`, or one well
outside 0..1, throws.
