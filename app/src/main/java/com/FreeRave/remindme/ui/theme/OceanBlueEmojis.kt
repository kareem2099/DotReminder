package com.FreeRave.remindme.ui.theme

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.FreeRave.remindme.data.ThemeSetting

// Premium Ocean-themed emojis with enhanced variety
object OceanEmojis {
    const val WAVE = "🌊"
    const val BIG_WAVE = "🌊🌊"
    const val FISH = "🐠"
    const val TROPICAL_FISH = "🐠"
    const val DOLPHIN = "🐬"
    const val OCTOPUS = "🐙"
    const val CRAB = "🦀"
    const val SEASHELL = "🐚"
    const val BEACH = "🏖️"
    const val PALM_TREE = "🌴"
    const val SUN = "☀️"
    const val WATER_DROP = "💧"
    const val STARFISH = "⭐"
    const val SEAHORSE = "🐴"
    const val WHALE = "🐋"
    const val SHARK = "🦈"
    const val TURTLE = "🐢"
    const val CORAL = "🪸"
    const val ANCHOR = "⚓"
    const val BOAT = "⛵"
    const val SPEEDBOAT = "🚤"
    const val SAILBOAT = "⛵"
    const val LIGHTHOUSE = "🏮"
    const val COMPASS = "🧭"
    const val SPARKLES = "✨"
    const val DIAMOND = "💎"
    const val PEARL = "🔘"
}

// Premium Forest-themed emojis with nature variety
object ForestEmojis {
    const val TREE = "🌳"
    const val BIG_TREE = "🌳🌲"
    const val LEAF = "🍃"
    const val FLOWER = "🌸"
    const val SQUIRREL = "🐿️"
    const val DEER = "🦌"
    const val OWL = "🦉"
    const val BUTTERFLY = "🦋"
    const val MUSHROOM = "🍄"
    const val HERB = "🌿"
    const val FOREST = "🌲"
    const val ACORN = "🌰"
    const val FOX = "🦊"
    const val RABBIT = "🐰"
    const val BIRD = "🐦"
    const val NEST = "🪺"
    const val SUNFLOWER = "🌻"
    const val ROSE = "🌹"
    const val HONEYBEE = "🐝"
    const val LADYBUG = "🐞"
    const val SPARKLES = "✨"
    const val DIAMOND = "💎"
}

// Premium Sunset Orange theme emojis with warm variety
object SunsetOrangeEmojis {
    const val SUN = "☀️"
    const val SUNSET = "🌅"
    const val FIRE = "🔥"
    const val CANDLE = "🕯️"
    const val GLOWING_STAR = "⭐"
    const val ORANGE_HEART = "🧡"
    const val TANGERINE = "🍊"
    const val PEACH = "🍑"
    const val HONEY = "🍯"
    const val BEE = "🐝"
    const val BUTTERFLY = "🦋"
    const val FLOWER = "🌸"
    const val ROSE = "🌹"
    const val SUNFLOWER = "🌻"
    const val CAMPFIRE = "🏕️"
    const val DESERT = "🏜️"
    const val LIGHTNING = "⚡"
    const val SPARKLES = "✨"
    const val DIAMOND = "💎"
}

// Premium Midnight Purple theme emojis with mystical night variety
object MidnightPurpleEmojis {
    const val MOON = "🌙"
    const val STAR = "⭐"
    const val GLOWING_STAR = "⭐"
    const val OWL = "🦉"
    const val BUTTERFLY = "🦋"
    const val MOON_WITH_STARS = "🌙"
    const val BAT = "🦇"
    const val OWL_AGAIN = "🦉"
    const val BUTTERFLY_AGAIN = "🦋"
    const val FULL_MOON = "🌕"
    const val CRESCENT_MOON = "🌙"
    const val STAR2 = "⭐"
    const val NIGHT_SKY = "🌌"
    const val COMET = "☄️"
    const val SPARKLES = "✨"
    const val CRYSTAL_BALL = "🔮"
    const val WAND = "🪄"
    const val GHOST = "👻"
    const val SKULL = "💀"
    const val COFFIN = "⚰️"
    const val DIAMOND = "💎"
}

// Premium Candy theme emojis with sweet treat variety
object CandyEmojis {
    const val LOLLIPOP = "🍭"
    const val CANDY = "🍬"
    const val LOLLIPOP_POP = "🍡"
    const val CAKE = "🎂"
    const val CUPCAKE = "🧁"
    const val COOKIE = "🍪"
    const val CHOCOLATE = "🍫"
    const val CANDY_AGAIN = "🍬"
    const val LOLLIPOP_AGAIN = "🍭"
    const val ICE_CREAM = "🍦"
    const val DONUT = "🍩"
    const val PIE = "🥧"
    const val BIRTHDAY_CAKE = "🎂"
    const val COOKIE2 = "🍪"
    const val CHOCOLATE_BAR = "🍫"
    const val SWEET = "🍬"
    const val CANDY_CANE = "🍭"
    const val SPARKLES = "✨"
    const val DIAMOND = "💎"
}

// Helper function to get enhanced text as string
fun getOceanEnhancedText(text: String, themeSetting: ThemeSetting): String {
    val isOceanBlue = themeSetting == ThemeSetting.OCEAN_BLUE
    val isForestGreen = themeSetting == ThemeSetting.FOREST_GREEN
    val isSunsetOrange = themeSetting == ThemeSetting.SUNSET_ORANGE
    val isMidnightPurple = themeSetting == ThemeSetting.MIDNIGHT_PURPLE
    val isCandy = themeSetting == ThemeSetting.CANDY

    return when {
        isOceanBlue -> {
            // Add ocean emojis to the text
            when {
                text.contains("Reminder") -> "$text ${OceanEmojis.WAVE}"
                text.contains("Add") -> "${OceanEmojis.SEASHELL} $text"
                text.contains("Settings") -> "$text ${OceanEmojis.STARFISH}"
                text.contains("Home") -> "${OceanEmojis.BEACH} $text"
                text.contains("AI") -> "${OceanEmojis.DOLPHIN} $text"
                else -> text
            }
        }
        isForestGreen -> {
            // Add forest emojis to the text
            when {
                text.contains("Reminder") -> "$text ${ForestEmojis.TREE}"
                text.contains("Add") -> "${ForestEmojis.LEAF} $text"
                text.contains("Settings") -> "$text ${ForestEmojis.SQUIRREL}"
                text.contains("Home") -> "${ForestEmojis.FOREST} $text"
                text.contains("AI") -> "${ForestEmojis.OWL} $text"
                else -> text
            }
        }
        isSunsetOrange -> {
            // Add sunset orange emojis to the text
            when {
                text.contains("Reminder") -> "$text ${SunsetOrangeEmojis.SUNSET}"
                text.contains("Add") -> "${SunsetOrangeEmojis.FIRE} $text"
                text.contains("Settings") -> "$text ${SunsetOrangeEmojis.GLOWING_STAR}"
                text.contains("Home") -> "${SunsetOrangeEmojis.SUN} $text"
                text.contains("AI") -> "${SunsetOrangeEmojis.SPARKLES} $text"
                else -> text
            }
        }
        isMidnightPurple -> {
            // Add midnight purple mystical emojis to the text
            when {
                text.contains("Reminder") -> "$text ${MidnightPurpleEmojis.MOON}"
                text.contains("Add") -> "${MidnightPurpleEmojis.STAR} $text"
                text.contains("Settings") -> "$text ${MidnightPurpleEmojis.OWL}"
                text.contains("Home") -> "${MidnightPurpleEmojis.NIGHT_SKY} $text"
                text.contains("AI") -> "${MidnightPurpleEmojis.SPARKLES} $text"
                else -> text
            }
        }
        isCandy -> {
            // Add candy sweet emojis to the text
            when {
                text.contains("Reminder") -> "$text ${CandyEmojis.LOLLIPOP}"
                text.contains("Add") -> "${CandyEmojis.CANDY} $text"
                text.contains("Settings") -> "$text ${CandyEmojis.CUPCAKE}"
                text.contains("Home") -> "${CandyEmojis.CAKE} $text"
                text.contains("AI") -> "${CandyEmojis.SPARKLES} $text"
                else -> text
            }
        }
        else -> text
    }
}

// Premium Ocean-themed text composable with enhanced styling
@Composable
fun OceanEmojiText(
    text: String,
    themeSetting: ThemeSetting,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    isPremium: Boolean = false
) {
    val isOceanBlue = themeSetting == ThemeSetting.OCEAN_BLUE

    if (isOceanBlue) {
        // Add ocean emojis to the text with premium variations
        val enhancedText = when {
            text.contains("Reminder") -> if (isPremium) "${OceanEmojis.SPARKLES} $text ${OceanEmojis.BIG_WAVE}" else "$text ${OceanEmojis.WAVE}"
            text.contains("Add") -> if (isPremium) "${OceanEmojis.SEASHELL}${OceanEmojis.SPARKLES} $text" else "${OceanEmojis.SEASHELL} $text"
            text.contains("Settings") -> if (isPremium) "$text ${OceanEmojis.STARFISH}${OceanEmojis.COMPASS}" else "$text ${OceanEmojis.STARFISH}"
            text.contains("Home") -> if (isPremium) "${OceanEmojis.BEACH}${OceanEmojis.PALM_TREE} $text" else "${OceanEmojis.BEACH} $text"
            text.contains("AI") -> if (isPremium) "${OceanEmojis.DOLPHIN}${OceanEmojis.SPARKLES} $text" else "${OceanEmojis.DOLPHIN} $text"
            else -> if (isPremium) "${OceanEmojis.WATER_DROP} $text" else text
        }

        val premiumStyle = if (isPremium) {
            style.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                fontSize = style.fontSize * 1.1f
            )
        } else {
            style.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.25.sp
            )
        }

        BasicText(
            text = enhancedText,
            modifier = modifier,
            style = premiumStyle
        )
    } else {
        BasicText(
            text = text,
            modifier = modifier,
            style = style
        )
    }
}

// Emoji icon replacements
@Composable
fun OceanThemedIcon(
    originalIcon: @Composable () -> Unit,
    themeSetting: ThemeSetting,
    oceanEmoji: String,
    modifier: Modifier = Modifier
) {
    if (themeSetting == ThemeSetting.OCEAN_BLUE) {
        BasicText(
            text = oceanEmoji,
            modifier = modifier,
            style = TextStyle(fontSize = 24.sp)
        )
    } else {
        originalIcon()
    }
}

// Decorative emoji elements
@Composable
fun OceanDecoration(
    themeSetting: ThemeSetting,
    position: DecorationPosition = DecorationPosition.TOP_RIGHT
) {
    if (themeSetting == ThemeSetting.OCEAN_BLUE) {
        val emoji = when (position) {
            DecorationPosition.TOP_LEFT -> OceanEmojis.PALM_TREE
            DecorationPosition.TOP_RIGHT -> OceanEmojis.SUN
            DecorationPosition.BOTTOM_LEFT -> OceanEmojis.SEASHELL
            DecorationPosition.BOTTOM_RIGHT -> OceanEmojis.STARFISH
        }

        BasicText(
            text = emoji,
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}

enum class DecorationPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
