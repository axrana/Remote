package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AcRemoteScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Custom spring-based scale animation for button presses.
 * Gives remote control buttons a very pleasant tactile/physical feedback feeling.
 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounce"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    onClick()
                }
            )
        }
}

@Composable
fun AcRemoteScreen(
    modifier: Modifier = Modifier,
    viewModel: RemoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle IR transmission toast notifications
    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title / Branding
        Text(
            text = "O GENERAL",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        ModelSelector(
            selectedModel = state.model,
            onModelSelected = { viewModel.setModel(it) }
        )

        // Simulated AC LCD Display Panel
        LcdScreenPanel(state = state)

        Spacer(modifier = Modifier.height(8.dp))

        // Physical remote chassis containing buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: Power ON/OFF Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PowerButton(
                    isOn = state.powerState,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.togglePower()
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // Row 2: Temperature Control
            TemperatureControlRow(
                temp = state.temperature,
                powerOn = state.powerState,
                onTempUp = {
                    if (state.powerState) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.incrementTemperature()
                    }
                },
                onTempDown = {
                    if (state.powerState) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.decrementTemperature()
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // Row 3: Operation Modes (Cool, Dry, Fan, Heat, Auto)
            ModeSelectorPanel(
                activeMode = state.mode,
                powerOn = state.powerState,
                onModeSelected = { mode ->
                    if (state.powerState) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setMode(mode)
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // Row 4: Fan Speed and Swing Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fan speed control
                Box(modifier = Modifier.weight(1f)) {
                    FanSpeedControl(
                        speed = state.fanSpeed,
                        powerOn = state.powerState,
                        onSpeedSelected = { speed ->
                            if (state.powerState) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setFanSpeed(speed)
                            }
                        }
                    )
                }

                // Swing toggle control
                Box(modifier = Modifier.weight(1f)) {
                    SwingToggleControl(
                        isSwingOn = state.swingState,
                        powerOn = state.powerState,
                        onToggle = {
                            if (state.powerState) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleSwing()
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // Row 5: Auxiliary controls (Turbo, Sleep, Timer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Turbo
                AuxButton(
                    text = "Turbo",
                    icon = Icons.Rounded.Bolt,
                    isActive = state.turboState,
                    enabled = state.powerState && (state.mode.lowercase() in listOf("cool", "heat", "auto")),
                    testTag = "turbo_button",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleTurbo()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Sleep
                AuxButton(
                    text = "Sleep",
                    icon = Icons.Rounded.NightsStay,
                    isActive = state.sleepState,
                    enabled = state.powerState,
                    testTag = "sleep_button",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSleep()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Timer
                AuxButton(
                    text = if (state.timerHours > 0) "${state.timerHours}h" else "Timer",
                    icon = Icons.Rounded.Schedule,
                    isActive = state.timerHours > 0,
                    enabled = state.powerState,
                    testTag = "timer_button",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.cycleTimer()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hardware details / Emulator simulation card
        HardwareInfoCard(irManager = viewModel.irRemoteManager)
    }
}

/**
 * Reusable LCD Screen Simulation component. Displays current state of the AC unit.
 */
@Composable
fun LcdScreenPanel(state: RemoteSettingsDataStore.AcSettings) {
    val lcdBackground = Color(0xFF0F1E1B)
    val lcdCyan = Color(0xFF00FFD1)
    val lcdDim = Color(0xFF1D3B36)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(lcdBackground)
            .border(
                width = 2.dp,
                color = Color(0xFF2C3E3A),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (!state.powerState) {
            // Screen is OFF
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STANDBY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = lcdDim,
                    letterSpacing = 4.sp
                )
            }
        } else {
            // Screen is ON
            // Top Row: Operation mode + Sleep / Turbo indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (state.mode.lowercase()) {
                            "cool" -> Icons.Rounded.AcUnit
                            "dry" -> Icons.Rounded.WaterDrop
                            "fan" -> Icons.Rounded.Air
                            "heat" -> Icons.Rounded.WbSunny
                            else -> Icons.Rounded.AutoMode
                        },
                        contentDescription = "Active Mode",
                        tint = lcdCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.mode.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = lcdCyan
                    )
                }

                // Auxiliary indicators (Turbo, Sleep, Timer)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TURBO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.turboState) lcdCyan else lcdDim
                    )
                    Text(
                        text = "SLEEP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.sleepState) lcdCyan else lcdDim
                    )
                    Text(
                        text = if (state.timerHours > 0) "TIMER ${state.timerHours}H" else "TIMER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.timerHours > 0) lcdCyan else lcdDim
                    )
                }
            }

            // Middle Row: Temperature Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.temperature}°C",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    color = lcdCyan,
                    modifier = Modifier.weight(1f)
                )

                // Swing Visual
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = "Swing State",
                        tint = if (state.swingState) lcdCyan else lcdDim,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "SWING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.swingState) lcdCyan else lcdDim
                    )
                }
            }

            // Bottom Row: Fan speed bars representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "FAN SPEED:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = lcdCyan
                    )
                    Text(
                        text = state.fanSpeed.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = lcdCyan
                    )
                }

                // Dynamic Graphic bars representing speed
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(16.dp)
                ) {
                    val activeColor = lcdCyan
                    val inactiveColor = lcdDim

                    val speedLower = state.fanSpeed.lowercase()
                    val autoMode = speedLower == "auto"

                    // Auto mode indicator block
                    Text(
                        text = "AUTO",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoMode) activeColor else inactiveColor,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    // Speed 1 (Low)
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(6.dp)
                            .background(if (!autoMode) activeColor else inactiveColor)
                    )
                    // Speed 2 (Medium)
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(10.dp)
                            .background(if (!autoMode && (speedLower == "medium" || speedLower == "high")) activeColor else inactiveColor)
                    )
                    // Speed 3 (High)
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(14.dp)
                            .background(if (!autoMode && speedLower == "high") activeColor else inactiveColor)
                    )
                }
            }
        }
    }
}

/**
 * High-Contrast Tactile Power Button
 */
@Composable
fun PowerButton(
    isOn: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isOn) Color(0xFFE04F2E) else MaterialTheme.colorScheme.surface
    val contentColor = if (isOn) Color.White else Color(0xFFE04F2E)

    Card(
        modifier = Modifier
            .size(72.dp)
            .bounceClick { onClick() }
            .testTag("power_button"),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = "Power Button",
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

/**
 * Segmented Temperature Up and Down Adjusters
 */
@Composable
fun TemperatureControlRow(
    temp: Int,
    powerOn: Boolean,
    onTempUp: () -> Unit,
    onTempDown: () -> Unit
) {
    val interactionAlpha = if (powerOn) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = interactionAlpha },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Temp Down Button
        Card(
            modifier = Modifier
                .size(64.dp)
                .bounceClick(enabled = powerOn) { onTempDown() }
                .testTag("temp_down_button"),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Decrease Temperature",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Large display number flanking
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TEMP",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "$temp°",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Temp Up Button
        Card(
            modifier = Modifier
                .size(64.dp)
                .bounceClick(enabled = powerOn) { onTempUp() }
                .testTag("temp_up_button"),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Increase Temperature",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Grid-like modern operation mode selectors
 */
@Composable
fun ModeSelectorPanel(
    activeMode: String,
    powerOn: Boolean,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf(
        "Auto" to Icons.Rounded.AutoMode,
        "Cool" to Icons.Rounded.AcUnit,
        "Dry" to Icons.Rounded.WaterDrop,
        "Fan" to Icons.Rounded.Air,
        "Heat" to Icons.Rounded.WbSunny
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "OPERATION MODE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { (modeName, icon) ->
                val isActive = activeMode.lowercase() == modeName.lowercase()
                val containerColor = if (powerOn && isActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
                val contentColor = if (powerOn && isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = if (powerOn) 0.8f else 0.4f)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor)
                        .border(
                            width = 1.dp,
                            color = if (powerOn && isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .bounceClick(enabled = powerOn) { onModeSelected(modeName) }
                        .testTag("mode_${modeName.lowercase()}_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = modeName,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = modeName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Fan speed switcher component
 */
@Composable
fun FanSpeedControl(
    speed: String,
    powerOn: Boolean,
    onSpeedSelected: (String) -> Unit
) {
    val speeds = listOf("Auto", "Low", "Medium", "High")
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "FAN SPEED",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .bounceClick(enabled = powerOn) { expanded = true }
                .testTag("fan_speed_dropdown"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Air,
                        contentDescription = "Fan Speed",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (powerOn) 0.8f else 0.4f)
                    )
                    Text(
                        text = speed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (powerOn) 0.8f else 0.4f)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select Speed",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (powerOn) 0.6f else 0.3f)
                )
            }

            DropdownMenu(
                expanded = expanded && powerOn,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                speeds.forEach { speedItem ->
                    DropdownMenuItem(
                        text = { Text(speedItem, fontWeight = FontWeight.Medium) },
                        onClick = {
                            onSpeedSelected(speedItem)
                            expanded = false
                        },
                        modifier = Modifier.testTag("fan_${speedItem.lowercase()}_item")
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Swing toggle component
 */
@Composable
fun SwingToggleControl(
    isSwingOn: Boolean,
    powerOn: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = if (powerOn && isSwingOn) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (powerOn && isSwingOn) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (powerOn) 0.8f else 0.4f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "SWING ACTION",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .border(
                    width = 1.dp,
                    color = if (powerOn && isSwingOn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .bounceClick(enabled = powerOn) { onToggle() }
                .testTag("swing_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Swing Toggle",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Text(
                    text = if (isSwingOn) "Swing ON" else "Swing OFF",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Reusable Auxiliary pill/action button for Turbo, Sleep, Timer
 */
@Composable
fun AuxButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (enabled && isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (enabled && isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.8f else 0.3f)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (enabled && isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .bounceClick(enabled = enabled) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Diagnostic card showing ConsumerIrManager status
 */
@Composable
fun HardwareInfoCard(irManager: IrRemoteManager) {
    val hasIr = irManager.hasIrEmitter()
    val frequencies = irManager.getSupportedFrequencies()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (hasIr) Icons.Rounded.CheckCircle else Icons.Rounded.OfflineBolt,
                contentDescription = "Hardware Info",
                tint = if (hasIr) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = if (hasIr) "IR Blaster: Connected" else "IR Blaster: Virtual Simulation Mode",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = if (hasIr) "Supported: $frequencies" else "Ready. Standard 38kHz transmission will log internally.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: String,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val models = listOf("AR-RAH2E", "AR-REB1E", "AR-RY4", "AR-REW4E", "AR-DB1", "AR-JW2")
    
    Box(contentAlignment = Alignment.Center) {
        InputChip(
            selected = true,
            onClick = { expanded = true },
            label = {
                Text(
                    text = "Remote Model: $selectedModel",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Select Model",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            },
            colors = InputChipDefaults.inputChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            models.forEach { model ->
                val cleanModel = model.replace("-", "")
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = model,
                            style = MaterialTheme.typography.bodyLarge
                        ) 
                    },
                    onClick = {
                        onModelSelected(cleanModel)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    }
}
