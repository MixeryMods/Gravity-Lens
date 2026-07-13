package com.example.mod.physics

/**
 * Entity-type exclusion filter driven by the Attunement enchantment
 * (Requirement 9 - Attunement). Level 0 == [ALL]; each higher level selects a
 * narrower target class group.
 */
enum class AttunementFilter {
    /** No restriction — pull everything eligible (default, Attunement 0). */
    ALL,

    /** Only projectiles (arrows, tridents, snowballs, potions, fireballs, ender pearls). */
    PROJECTILES_ONLY,

    /** Only mobs and players. */
    MOBS_ONLY,

    /** Only dropped item entities. */
    ITEMS_ONLY,
    ;

    companion object {
        /** Map an Attunement enchantment level to a filter, clamping to range. */
        fun fromLevel(level: Int): AttunementFilter {
            val values = entries
            return values[level.coerceIn(0, values.size - 1)]
        }
    }
}
