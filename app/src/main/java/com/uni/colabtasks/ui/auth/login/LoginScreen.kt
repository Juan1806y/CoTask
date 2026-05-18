package com.uni.colabtasks.ui.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uni.colabtasks.R
import com.uni.colabtasks.ui.auth.GoogleSignInHelper
import kotlinx.coroutines.launch

private val OrangeBrand = Color(0xFFFF7A2F)
private val BlueDark    = Color(0xFF1B3A5C)
private val OrangeLight = Color(0xFFFFAA80)

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onGoToSignUp: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val googleHelper = remember { GoogleSignInHelper(context, webClientId) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Fondo naranja superior con patrón topográfico ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.52f)
                    .background(OrangeBrand)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawTopoLines(this)
                }
            }

            // ── Fondo azul inferior con ola ──
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            ) {
                val waveY = size.height * 0.48f
                val path = Path().apply {
                    moveTo(0f, waveY + size.height * 0.04f)
                    cubicTo(
                        size.width * 0.25f, waveY - size.height * 0.04f,
                        size.width * 0.6f,  waveY + size.height * 0.06f,
                        size.width,         waveY
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, color = BlueDark)
            }

            // ── Contenido ──
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))

                // Logo / título
                Text(
                    text = "CoTask",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(16.dp))

                // Sticky notes
                StickyNotesIllustration()

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Notas Colaborativas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(Modifier.height(32.dp))

                // ── Tarjeta de login ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "INGRESAR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Campo Email
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = { Text(stringResource(R.string.email), color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, null, tint = Color.White.copy(alpha = 0.8f))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = OrangeLight,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            cursorColor          = OrangeLight,
                            focusedContainerColor   = Color.White.copy(alpha = 0.18f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.18f),
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Campo Contraseña
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = { Text(stringResource(R.string.password), color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, null, tint = Color.White.copy(alpha = 0.8f))
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = OrangeLight,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            cursorColor          = OrangeLight,
                            focusedContainerColor   = Color.White.copy(alpha = 0.18f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.18f),
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // Botón Iniciar sesión
                    Button(
                        onClick = viewModel::signIn,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor   = Color(0xFF3A1A00)
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF3A1A00),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sign_in),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Divisor
                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f))

                    Spacer(Modifier.height(16.dp))

                    // Botón Google
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                when (val r = googleHelper.requestIdToken()) {
                                    is GoogleSignInHelper.Result.Success ->
                                        viewModel.signInWithGoogleToken(r.idToken)
                                    is GoogleSignInHelper.Result.Failure ->
                                        viewModel.reportGoogleFailure(r.message)
                                }
                            }
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor   = Color(0xFF3A1A00)
                        ),
                        border = null
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified  // ← importante: sin esto se pinta de un solo color
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.continue_with_google),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Link registro
                    TextButton(onClick = onGoToSignUp) {
                        Text(
                            text = stringResource(R.string.no_account_prompt),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Líneas topográficas ──
private fun drawTopoLines(scope: DrawScope) {
    val paint = Color.White.copy(alpha = 0.12f)
    val stroke = Stroke(width = 1.5f)
    val w = scope.size.width
    val h = scope.size.height
    val curves = listOf(
        listOf(Offset(0f, h*0.2f), Offset(w*0.3f, h*0.1f), Offset(w*0.7f, h*0.25f), Offset(w, h*0.15f)),
        listOf(Offset(0f, h*0.38f), Offset(w*0.2f, h*0.28f), Offset(w*0.5f, h*0.44f), Offset(w*0.8f, h*0.3f), Offset(w, h*0.38f)),
        listOf(Offset(0f, h*0.55f), Offset(w*0.4f, h*0.46f), Offset(w*0.6f, h*0.58f), Offset(w, h*0.5f)),
        listOf(Offset(w*0.1f, 0f), Offset(w*0.2f, h*0.18f), Offset(w*0.35f, h*0.06f)),
        listOf(Offset(w*0.6f, 0f), Offset(w*0.68f, h*0.22f), Offset(w*0.88f, h*0.1f), Offset(w, h*0.24f)),
    )
    curves.forEach { pts ->
        if (pts.size < 2) return@forEach
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            if (pts.size == 2) {
                lineTo(pts[1].x, pts[1].y)
            } else {
                for (i in 1 until pts.size - 1) {
                    val cx = (pts[i].x + pts[i+1].x) / 2
                    val cy = (pts[i].y + pts[i+1].y) / 2
                    quadraticTo(pts[i].x, pts[i].y, cx, cy)
                }
                lineTo(pts.last().x, pts.last().y)
            }
        }
        scope.drawPath(path, color = paint, style = stroke)
    }
}

// ── Post-its ──
@Composable
fun StickyNotesIllustration() {
    Box(
        modifier = Modifier.size(width = 160.dp, height = 140.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(115.dp)
                .offset(x = 10.dp, y = 7.dp)
                .graphicsLayer { rotationZ = 6f }
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(Color(0xFFF5E98A), RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .size(115.dp)
                .graphicsLayer { rotationZ = -3f }
                .shadow(6.dp, RoundedCornerShape(4.dp))
                .background(Color(0xFFFAF0A0), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-5).dp)
                    .background(Color(0xFFD32F2F), shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}