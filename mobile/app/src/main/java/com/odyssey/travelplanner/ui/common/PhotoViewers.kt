package com.odyssey.travelplanner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import com.odyssey.travelplanner.data.Sight
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedSightName
import com.odyssey.travelplanner.ui.screen.trip.sights.rememberSightBitmap
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.primaryContentColor

@Composable
internal fun FullScreenSightPhotoViewer(
    sight: com.odyssey.travelplanner.data.Sight,
    onDismiss: () -> Unit,
) {
    val bitmap = rememberSightBitmap(sight)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = localizedSightName(sight.name),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(34.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { onDismiss() },
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                        tint = primaryContentColor(),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun FullScreenPhotoViewer(
    photos: List<Any>,
    initialIndex: Int,
    accommodationName: String,
    onDismiss: (Int) -> Unit,
) {
    var photoIndex by remember(photos, initialIndex) {
        mutableStateOf(initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onDismiss(photoIndex) },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            if (dialogWindow == null) {
                return@DisposableEffect onDispose { }
            }
            val previousStatusBarColor = dialogWindow.statusBarColor
            val previousNavigationBarColor = dialogWindow.navigationBarColor
            val previousDimAmount = dialogWindow.attributes.dimAmount
            val insetsController = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
            val previousLightStatusBars = insetsController.isAppearanceLightStatusBars
            val previousLightNavigationBars = insetsController.isAppearanceLightNavigationBars

            dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow.setDimAmount(1f)
            dialogWindow.statusBarColor = android.graphics.Color.BLACK
            dialogWindow.navigationBarColor = android.graphics.Color.BLACK
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false

            onDispose {
                dialogWindow.setDimAmount(previousDimAmount)
                dialogWindow.statusBarColor = previousStatusBarColor
                dialogWindow.navigationBarColor = previousNavigationBarColor
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, true)
                insetsController.isAppearanceLightStatusBars = previousLightStatusBars
                insetsController.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AsyncImage(
                model = photos[photoIndex],
                contentDescription = accommodationName,
                // Keep the complete photo visible; black letterbox bands are
                // intentional and the dimmed dialog background hides the app below.
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { onDismiss(photoIndex) },
                ) {
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            if (photos.size > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { photoIndex = (photoIndex - 1 + photos.size) % photos.size },
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Предыдущее фото", "Previous photo", "Foto anterior", "Vorheriges Foto"), tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { photoIndex = (photoIndex + 1) % photos.size },
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Следующее фото", "Next photo", "Foto siguiente", "Nächstes Foto"), tint = Color.White, modifier = Modifier.size(22.dp).graphicsLayer(rotationZ = 180f))
                }
                Text(
                    text = "${photoIndex + 1}/${photos.size}",
                    color = Color.White,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                        .background(Color(0xAA0F0F19), RoundedCornerShape(16.dp))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }
        }
    }
}

