package com.r2r.readytorevise.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {

    val context = LocalContext.current

    val logo = remember {
        loadLogoFromAssets(context)
    }

    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    val rotation = remember { Animatable(-5f) }
    val shimmer = remember { Animatable(-500f) }

    LaunchedEffect(Unit) {

        launch {
            alpha.animateTo(
                1f,
                animationSpec = tween(600)
            )
        }

        launch {
            scale.animateTo(
                1f,
                animationSpec = tween(
                    800,
                    easing = FastOutSlowInEasing
                )
            )
        }

        launch {
            rotation.animateTo(
                0f,
                animationSpec = tween(800)
            )
        }

        delay(700)

        shimmer.animateTo(
            500f,
            animationSpec = tween(
                700,
                easing = LinearEasing
            )
        )

        delay(600)

        onFinished()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {


        // Logo
        Image(
            bitmap = logo,
            contentDescription = "App Logo",
            modifier = Modifier
                .size(170.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = rotation.value
                }
                .alpha(alpha.value),
            contentScale = ContentScale.Fit
        )


        // Light sweep effect
        Box(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    translationX = shimmer.value
                    rotationZ = -25f
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}


fun loadLogoFromAssets(context: Context): ImageBitmap {
    val bitmap = BitmapFactory.decodeStream(
        context.assets.open("AppLogo2(Light).png")
    )
    return bitmap?.asImageBitmap() ?: ImageBitmap(1, 1)
}