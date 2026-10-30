package ch.wuethrich.greenprint.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.ic_room_bedroom
import greenprint.composeapp.generated.resources.ic_room_corridor
import greenprint.composeapp.generated.resources.ic_room_kitchen
import greenprint.composeapp.generated.resources.ic_room_living
import greenprint.composeapp.generated.resources.ic_room_toilet
import greenprint.composeapp.generated.resources.ic_space_apartment
import greenprint.composeapp.generated.resources.ic_space_house
import greenprint.composeapp.generated.resources.ic_space_office
import org.jetbrains.compose.resources.painterResource

object IconRegistry {
    val spaceIcons = listOf("house_24", "apartment_24", "office_24")
    val roomIcons = listOf("living_24", "bedroom_24", "kitchen_24", "toilet_24", "corridor_24")

    @Composable
    fun getPainter(iconId: String?): Painter = when (iconId) {
        "house_24" -> painterResource(Res.drawable.ic_space_house)
        "apartment_24" -> painterResource(Res.drawable.ic_space_apartment)
        "office_24" -> painterResource(Res.drawable.ic_space_office)
        "living_24" -> painterResource(Res.drawable.ic_room_living)
        "bedroom_24" -> painterResource(Res.drawable.ic_room_bedroom)
        "kitchen_24" -> painterResource(Res.drawable.ic_room_kitchen)
        "toilet_24" -> painterResource(Res.drawable.ic_room_toilet)
        "corridor_24" -> painterResource(Res.drawable.ic_room_corridor)
        else -> painterResource(Res.drawable.ic_leaf) // Default fallback
    }
}
