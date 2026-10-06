# CLAUDE.md

Project background, screenshots and run instructions for humans are in `README.md`; this file is exclusively for  
Claude Code.

## Commands

```bash
./gradlew build
./gradlew test --tests boids.KDTreeTest
./gradlew test --tests 'boids.KDTreeTest.finds the neighbors within a radius'   # one test (backticked name)
```

- `-q` hides test counts, and an up-to-date `test` task runs nothing. To confirm what ran, add `--rerun-tasks` and read
  `build/test-results/test/TEST-<class>.xml` (`tests=`, `failures=`).
- `./gradlew run` (and `-Popenrndr.application=voronoiboids.VoronoiFlockingKt`) opens a GUI window and blocks until it
  is closed. Do not use it to verify changes; use `build` and tests, and ask the user to check visual behaviour.
- `openrndrRelease`, `openrndrSonatypeSnapshot` and `openrndrLocalSnapshot` query GitHub and rewrite
  `gradle/libs.versions.toml` and `gradle.properties`. Run them only when asked.

## Constraints

- Tests use JUnit 4 (`org.junit.Test`, `org.junit.Assert`).
- `settings.gradle.kts` reads the OPENRNDR and ORX versions by regex from the `openrndr = "..."` and `orx = "..."` lines
  in `gradle/libs.versions.toml`. Keep those lines in that plain form.
- All three programs `extend(Screenshots())`, which writes PNGs into `screenshots/`. `README.md` embeds files from there, so
  do not delete or rename them without checking the README.

## Behaviour not obvious from any one file

- `voronoiboids/VoronoiFlocking.kt` and `sidebyside/SideBySide.kt` get Voronoi cells back in `flock.boids` order and
  `CellDrawer` pairs them with boids by index, so nothing may reorder `flock.boids` between `flock.run` and drawing.
- `Boid.align` and `Boid.cohesion` do no distance filtering; they trust the neighbour list. The query radius in `Flock`
  is therefore their radius, and changing it changes behaviour. Only `separate` filters again.
- Boids update in place in sequence, so later boids see earlier boids' new state, while the `KDTree` holds
  start-of-frame positions.
- `Boid`'s initial heading comes from unseeded `Double.uniform`, so tests of steering behaviour are not deterministic.
  The drawers need a GL context and cannot be unit-tested here.
- `speedColor` does not clamp: a NaN `speedFraction`, or one well outside 0..1, throws. It is called while drawing, so
  the failure shows only in the running GUI.
