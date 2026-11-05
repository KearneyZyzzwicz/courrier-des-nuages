package fr.courrierdesnuages.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.courrierdesnuages.app.R

val Encre = Color(0xFF1E3A5F)
val EncreDouce = Color(0xFF4A5D7A)
val Rouge = Color(0xFFE4573D)
val RougeFonce = Color(0xFFC8432C)
val Jaune = Color(0xFFF2C14E)
val Vert = Color(0xFF3E9A6A)
val Creme = Color(0xFFFFFDF8)
val CielHaut = Color(0xFF8FCDEB)
val CielBas = Color(0xFFE3F3FA)
val Nuage = Color(0xFFF6FBFE)

/** Fredoka pour les titres et les boutons, Andika (lettres faciles à lire) pour les textes. */
val Fredoka = FontFamily(
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
    Font(R.font.fredoka_bold, FontWeight.Bold),
)

val Andika = FontFamily(
    Font(R.font.andika_regular, FontWeight.Normal),
    Font(R.font.andika_bold, FontWeight.Bold),
)

private val Couleurs = lightColorScheme(
    primary = Rouge,
    onPrimary = Color.White,
    secondary = Encre,
    onSecondary = Color.White,
    tertiary = Jaune,
    background = Creme,
    onBackground = Encre,
    surface = Color.White,
    onSurface = Encre,
    surfaceVariant = CielBas,
    onSurfaceVariant = EncreDouce,
    outline = EncreDouce,
    error = RougeFonce,
)

private val Typo = Typography(
    displaySmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Andika, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Andika, fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Andika, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
)

@Composable
fun ThemeCourrier(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Couleurs, typography = Typo, content = content)
}
