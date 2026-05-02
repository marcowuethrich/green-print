package ch.wuethrich.greenprint.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import greenprint.composeapp.generated.resources.Res
import greenprint.composeapp.generated.resources.ic_leaf
import greenprint.composeapp.generated.resources.ic_space_apartment
import greenprint.composeapp.generated.resources.ic_space_house
import greenprint.composeapp.generated.resources.ic_space_office
import greenprint.composeapp.generated.resources.ic_room_bed
import greenprint.composeapp.generated.resources.ic_room_kitchen
import greenprint.composeapp.generated.resources.ic_room_sofa
import org.jetbrains.compose.resources.painterResource

object IconRegistry {
    val spaceIcons = listOf("house_24", "apartment_24", "office_24")
    val roomIcons = listOf("sofa_24", "bed_24", "kitchen_24")

    @Composable
    fun getPainter(iconId: String?): Painter {
        return when (iconId) {
            "house_24" -> painterResource(Res.drawable.ic_space_house)
            "apartment_24" -> painterResource(Res.drawable.ic_space_apartment)
            "office_24" -> painterResource(Res.drawable.ic_space_office)
            "sofa_24" -> painterResource(Res.drawable.ic_room_sofa)
            "bed_24" -> painterResource(Res.drawable.ic_room_bed)
            "kitchen_24" -> painterResource(Res.drawable.ic_room_kitchen)
            else -> painterResource(Res.drawable.ic_leaf) // Default fallback
        }
    }
}
