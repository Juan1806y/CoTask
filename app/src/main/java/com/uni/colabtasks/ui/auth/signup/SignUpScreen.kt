package com.uni.colabtasks.ui.auth.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uni.colabtasks.R

private val OrangeBrand = Color(0xFFFF7A2F)
private val BlueDark    = Color(0xFF1B3A5C)
private val OrangeLight = Color(0xFFFFAA80)

@Composable
fun SignUpScreen(
    onSignedUp: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.signedUp) { if (state.signedUp) onSignedUp() }
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
                    .fillMaxHeight(0.42f)
                    .background(OrangeBrand)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawTopoLinesSignUp(this)
                }
            }

            // ── Fondo azul inferior con ola ──
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            ) {
                val waveY = size.height * 0.38f
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

            // ── Contenido scrollable ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Cabecera naranja ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 16.dp),
                ) {
                    // Botón atrás
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    // Título centrado
                    Text(
                        text = "CoTask",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Mini sticky note decorativo
                MiniStickyNote()

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.signup_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(Modifier.height(28.dp))

                // ── Formulario ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "CREAR CUENTA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Campo Nombre
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = viewModel::onNameChange,
                        placeholder = {
                            Text(
                                stringResource(R.string.display_name),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Person,
                                null,
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Campo Email
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = {
                            Text(
                                stringResource(R.string.email),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Email,
                                null,
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Campo Contraseña
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = {
                            Text(
                                stringResource(R.string.password),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Lock,
                                null,
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    // Botón Crear cuenta
                    Button(
                        onClick = viewModel::signUp,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeLight,
                            contentColor   = Color(0xFF3A1A00)
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(20.dp),
                                color     = Color(0xFF3A1A00),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text       = stringResource(R.string.sign_up),
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f))

                    Spacer(Modifier.height(16.dp))

                    // Link volver al login
                    TextButton(onClick = onBack) {
                        Text(
                            text     = stringResource(R.string.has_account_prompt),
                            color    = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Colores compartidos para los campos ──
@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = OrangeLight,
    unfocusedBorderColor    = Color.White.copy(alpha = 0.4f),
    focusedTextColor        = Color.White,
    unfocusedTextColor      = Color.White,
    cursorColor             = OrangeLight,
    focusedContainerColor   = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
)

// ── Sticky note pequeño decorativo ──
@Composable
private fun MiniStickyNote() {
    Box(
        modifier = Modifier.size(width = 110.dp, height = 95.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 7.dp, y = 5.dp)
                .graphicsLayer { rotationZ = 6f }
                .shadow(3.dp, RoundedCornerShape(4.dp))
                .background(Color(0xFFF5E98A), RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { rotationZ = -3f }
                .shadow(5.dp, RoundedCornerShape(4.dp))
                .background(Color(0xFFFAF0A0), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
                    .background(Color(0xFFD32F2F), shape = CircleShape)
            )
        }
    }
}

// ── Líneas topográficas ──
private fun drawTopoLinesSignUp(scope: DrawScope) {
    val paint  = Color.White.copy(alpha = 0.12f)
    val stroke = Stroke(width = 1.5f)
    val w = scope.size.width
    val h = scope.size.height
    val curves = listOf(
        listOf(Offset(0f, h*0.2f),  Offset(w*0.3f, h*0.1f),  Offset(w*0.7f, h*0.25f), Offset(w, h*0.15f)),
        listOf(Offset(0f, h*0.42f), Offset(w*0.2f, h*0.3f),  Offset(w*0.5f, h*0.48f), Offset(w*0.8f, h*0.34f), Offset(w, h*0.42f)),
        listOf(Offset(0f, h*0.62f), Offset(w*0.4f, h*0.52f), Offset(w*0.6f, h*0.65f), Offset(w, h*0.55f)),
        listOf(Offset(w*0.1f, 0f),  Offset(w*0.2f, h*0.2f),  Offset(w*0.38f, h*0.07f)),
        listOf(Offset(w*0.62f, 0f), Offset(w*0.7f, h*0.24f), Offset(w*0.9f, h*0.12f), Offset(w, h*0.26f)),
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