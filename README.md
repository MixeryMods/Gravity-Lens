# Gravity Lens

A Fabric mod for **Minecraft 26.2** (Kotlin, Java 25) that adds the **Gravity Lens** — a throwable, retrievable item that sticks to any surface and projects a redstone-tunable spherical gravity field. The field additively bends the velocity of nearby entities and projectiles, letting you curve arrows, redirect mobs, and pull dropped items through the air — all without touching a single block of terrain.

## Features

- **Throw & stick** — right-click to throw the lens; on impact with any floor, wall, or ceiling it anchors in place and begins projecting its field.
- **Spherical gravity field** — every server tick, entities within the sphere are pulled toward the center with a force that scales inversely with distance (strongest at the core, zero at the rim).
- **Purely additive physics** — the pull is *added* to an entity's existing motion, never overwritten, so trajectories curve smoothly instead of snapping.
- **Multi-lens stacking** — overlapping fields combine independently. Two offset lenses can route a fast projectile around an obstacle.
- **Redstone control** — power the anchor block to tune pull strength from 1–15; signal 0 halts the field entirely, in real time.
- **Mass-aware** — lightweight projectiles and items curve hard; heavier mobs resist more.
- **Retrieval & lifetime** — lenses last 30 seconds by default. Recall one by interacting with it (empty-handed or otherwise); retrieval ends the field instantly, leaving orbiting entities on their tangent.

## Enchantments

| Enchantment | Effect |
|---|---|
| **Resonance** | Increases the field radius (and pull strength) per level. |
| **Attunement** | Restricts the pull to a single class of target — projectiles only, mobs only, or items only. |
| **Persistence** | Extends the deployed lifetime countdown. |

## Crafting

Shaped recipe:

```
P S P
S E S
P S P
```

- **P** — Glass Pane
- **S** — Amethyst Shard
- **E** — Ender Pearl

## Requirements

- Minecraft **26.2**
- Fabric Loader **0.19.3+**
- Fabric API **0.153.0+26.2**
- Fabric Language Kotlin **1.13.12+kotlin.2.4.0**
- Java **25**

## Building

```sh
./gradlew build
```

The mod jar is produced at `build/libs/Gravity-Lens.26.2.jar`. Run the physics tests with `./gradlew test`.

## License

Licensed under the **BSD 4-Clause License** — see [LICENSE](LICENSE).

---

Created by **Mixery**.
