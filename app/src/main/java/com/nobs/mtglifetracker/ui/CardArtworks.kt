package com.nobs.mtglifetracker.ui

import androidx.annotation.DrawableRes
import com.nobs.mtglifetracker.R

data class CardArtwork(val id: String, val name: String, val artist: String, @DrawableRes val resource: Int)

object CardArtworks {
    val all = listOf(
        CardArtwork("juzam_djinn", "Juzám Djinn", "Mark Tedin", R.drawable.art_juzam_djinn),
        CardArtwork("ancestral_recall", "Ancestral Recall", "Ryan Pancoast", R.drawable.art_ancestral_recall),
        CardArtwork("intuition", "Intuition", "April Lee", R.drawable.art_intuition),
        CardArtwork("survival_of_the_fittest", "Survival of the Fittest", "Pete Venters", R.drawable.art_survival_of_the_fittest),
        CardArtwork("squee_goblin_nabob", "Squee, Goblin Nabob", "David Monette", R.drawable.art_squee_goblin_nabob),
        CardArtwork("black_lotus", "Black Lotus", "Chris Rahn", R.drawable.art_black_lotus),
        CardArtwork("birds_of_paradise", "Birds of Paradise", "Marcelo Vignali", R.drawable.art_birds_of_paradise),
        CardArtwork("lightning_bolt", "Lightning Bolt", "Christopher Moeller", R.drawable.art_lightning_bolt),
        CardArtwork("serra_angel", "Serra Angel", "Greg Staples", R.drawable.art_serra_angel),
        CardArtwork("necropotence", "Necropotence", "Dave Kendall", R.drawable.art_necropotence),
    )

    fun find(id: String?): CardArtwork? = all.firstOrNull { it.id == id }
}
