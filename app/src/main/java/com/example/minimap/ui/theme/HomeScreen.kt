package com.example.minimap.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.minimap.autowide
import com.example.minimap.data.preferences.SettingsRepository
import com.example.minimap.model.Screen
import kotlinx.coroutines.delay


// File to handle home screen, the application home


object AppState {
    var isFirstLaunch by mutableStateOf(true)
}



@Composable
fun HomeScreen(navController: NavController) {
    var showTitle by remember { mutableStateOf(false) }
    var showUserInfo by remember { mutableStateOf(false) }
    var showScanButton by remember { mutableStateOf(false) }
    var showBottomButtons by remember { mutableStateOf(false) }

    val showVersionEnabledState by SettingsRepository(LocalContext.current).showVersionEnabledFlow.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        if (AppState.isFirstLaunch) {
            // First launch squential animation
            showTitle = true
            delay(900)
            showUserInfo = true
            delay(400)
            showScanButton = true
            delay(400)
            showBottomButtons = true
            delay(700)
            AppState.isFirstLaunch = false
        } else {
            // Next launch, direct display without animation
            showTitle = true
            showUserInfo = true
            showScanButton = true
            showBottomButtons = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        // Depth effect with gradient circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width/2, size.height/2)
            val maxRadius = size.maxDimension

            for (i in 1..5) {
                val radius = maxRadius * (i/5f)
                drawCircle(
                    color = Color(0xFF111111).copy(alpha = 1f - (i * 0.15f)),
                    center = center,
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Title at the top
        AnimatedVisibility(
            visible = showTitle,
            enter = if (AppState.isFirstLaunch) fadeIn() + expandVertically() else fadeIn(animationSpec = tween(0)),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TerminalTitle(animate = AppState.isFirstLaunch,
                showVersion = showVersionEnabledState)
        }

        // User info with WiFi circles
        AnimatedVisibility(
            visible = showUserInfo,
            enter = if (AppState.isFirstLaunch) fadeIn() + expandVertically() else fadeIn(animationSpec = tween(0)),
            exit = fadeOut(),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            UserInfoWithWifiIndicator()
        }

        // SCAN button with animation
        AnimatedVisibility(
            visible = showScanButton,
            enter = if (AppState.isFirstLaunch) fadeIn() + expandVertically() else fadeIn(animationSpec = tween(0)),
            modifier = Modifier.fillMaxSize(),
            exit = fadeOut()
        ) {
            Box(contentAlignment = Alignment.Center) {
                ScanButton(navController)
            }
        }

        // Bottom button with animation
        AnimatedVisibility(
            visible = showBottomButtons,
            enter = if (AppState.isFirstLaunch) fadeIn() + expandVertically(expandFrom = Alignment.Bottom) else fadeIn(animationSpec = tween(0)),
            modifier = Modifier.align(Alignment.BottomCenter),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton("ooo", { navController.navigate("parameterViewer") })
                IconButton("|||\\", { navController.navigate("fileViewer") })
            }
        }
    }
}

@Composable
private fun ScanButton(navController: NavController) {

    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .background(
                color = Color(0xFF1E2624).copy(alpha = 0.8f),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = Color(0xFF00FF00).copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { navController.navigate(Screen.WifiScan.route) }
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SCAN",
                color = Color.Green.copy(alpha = 0.9f),
                fontFamily = autowide,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.shadow(2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "WiFi Networks",
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = autowide,
                fontSize = 14.sp
            )
        }
    }
}


@Composable
private fun TerminalTitle(animate: Boolean = true, showVersion: Boolean = false) {
    var showCursor by remember { mutableStateOf(false) }
    var textToDisplay by remember { mutableStateOf(if (animate) "" else "MINIMAP32") }
    var showPrefix by remember { mutableStateOf(animate) }
    val fullText = "MINIMAP32"
    val prefix = "$> "
    var animationComplete by remember { mutableStateOf(!animate) }


    // Retrieve current application version
    val packageInfo = LocalContext.current.packageManager
        .getPackageInfo(LocalContext.current.packageName, 0)
    val versionName = packageInfo.versionName ?: "N/A"

    LaunchedEffect(animate) {
        if (animate) {
            showCursor = true
            fullText.forEachIndexed { index, _ ->
                textToDisplay = fullText.take(index + 1)
                delay(150)
            }
            animationComplete = true
            delay(500)
            showCursor = false
            showPrefix = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showPrefix) {
                Text(
                    text = prefix,
                    color = Color(0xFF00FF00),
                    fontFamily = autowide,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.shadow(4.dp)
                )
            }

            Text(
                text = textToDisplay,
                color = Color(0xFF00FF00),
                fontFamily = autowide,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.shadow(4.dp),
                textAlign = TextAlign.Center
            )

            if (showCursor && !animationComplete) {
                AnimatedVisibility(
                    visible = showCursor,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 48.dp)
                            .background(Color(0xFF00FF00))
                    )
                }
            }

        }

        // Display version if showVersion is true
        if (showVersion) {
            Text(
                text = "v$versionName",
                color = Color.Green.copy(alpha = 0.7f),
                fontFamily = autowide,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


@Composable
fun UserInfoWithWifiIndicator() {

    val context = LocalContext.current

    val wifiNetworks by remember {
        mutableStateOf(readWifiNetworksFromCsv(context, "wifis_dataset.csv"))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Text "User's analysis:"
        Text(
            text = "User's analysis:",
            color = Color.White,
            fontFamily = autowide,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Wifi visual indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            // Concentric circles
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                // small inner circle
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2
                    )
                }

                // medium circle
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // big outer circle
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }


            Text(
                text = "${wifiNetworks.size} observed wifi",
                color = Color.Green,
                fontFamily = autowide,
                fontSize = 14.sp
            )
        }
    }
}


@Composable
private fun IconButton(icon: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(60.dp)
            .background(
                color = Color(0xFF232222),
                shape = CircleShape
            )
            .clickable { onClick() }
    ) {
        Text(icon, color = Color.Green, fontSize = 24.sp)
    }
}


