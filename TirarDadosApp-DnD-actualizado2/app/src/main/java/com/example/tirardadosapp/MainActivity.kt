package com.example.tirardadosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val LIMITE_DADOS = 100
private const val DURACION_TIRADA_MS = 1_650L
private const val MAX_DADOS_ANIMADOS = 24

private data class OpcionDado(val etiqueta: String, val caras: Int, val recursoImagen: Int)

private val opcionesDados = listOf(
    OpcionDado("D4", 4, R.drawable.dado_d4),
    OpcionDado("D6", 6, R.drawable.dado_d6),
    OpcionDado("D8", 8, R.drawable.dado_d8),
    OpcionDado("D10", 10, R.drawable.dado_d10),
    OpcionDado("D12", 12, R.drawable.dado_d12),
    OpcionDado("D20", 20, R.drawable.dado_d20),
    OpcionDado("D100", 100, R.drawable.dado_d20)
)

private data class ConfiguracionTirada(
    val id: Int,
    val cantidad: String = "",
    val modificador: String = "",
    val dado: OpcionDado = opcionesDados.last(),
    val menuAbierto: Boolean = false
)

private data class ResultadoTirada(
    val configuracion: ConfiguracionTirada,
    val valores: List<Int>
)

private data class DadoFisico(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotacion: Float,
    val velocidadRotacion: Float,
    val rebotes: Int,
    val caras: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PantallaPrincipal() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    var configuraciones by remember { mutableStateOf(listOf(ConfiguracionTirada(id = 0))) }
    var siguienteId by remember { mutableIntStateOf(1) }
    var resultados by remember { mutableStateOf(emptyList<ResultadoTirada>()) }
    var tirando by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val fondo = Brush.verticalGradient(
        listOf(Color(0xFF2B1B12), Color(0xFF3E2723), Color(0xFF1C0F0A))
    )
    val pergamino = Color(0xFFF2E0B6)
    val madera = Color(0xFF5D4037)
    val dorado = Color(0xFFC9A227)
    val critFail = Color(0xFFB00020)
    val critSuccess = Color(0xFF2E7D32)

    fun actualizarConfiguracion(
        id: Int,
        cambio: (ConfiguracionTirada) -> ConfiguracionTirada
    ) {
        configuraciones = configuraciones.map { actual ->
            if (actual.id == id) cambio(actual) else actual
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)
                .padding(padding)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "🍺 Tirar Dados 🎲",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = dorado
                )
                Text("Que la suerte decida tu destino", color = Color(0xFFFFE9C4))

                configuraciones.forEachIndexed { indice, configuracion ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = pergamino),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (configuraciones.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tirada ${indice + 1}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = madera
                                    )
                                    if (indice > 0) {
                                        TextButton(
                                            enabled = !tirando,
                                            onClick = {
                                                configuraciones = configuraciones.filterNot {
                                                    it.id == configuracion.id
                                                }
                                            }
                                        ) { Text("Eliminar", color = madera) }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = configuracion.cantidad,
                                onValueChange = { valor ->
                                    actualizarConfiguracion(configuracion.id) {
                                        it.copy(cantidad = valor.filter(Char::isDigit).take(3))
                                    }
                                },
                                label = { Text("Cantidad de dados (máx. $LIMITE_DADOS)") },
                                enabled = !tirando,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = configuracion.modificador,
                                onValueChange = { valor ->
                                    val limpio = valor.filterIndexed { index, caracter ->
                                        caracter.isDigit() ||
                                            ((caracter == '-' || caracter == '+') && index == 0)
                                    }.take(6)
                                    actualizarConfiguracion(configuracion.id) {
                                        it.copy(modificador = limpio)
                                    }
                                },
                                label = { Text("Modificador (opcional)") },
                                supportingText = { Text("Se suma al total de la tirada") },
                                enabled = !tirando,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = configuracion.menuAbierto,
                                onExpandedChange = { abierto ->
                                    if (!tirando) actualizarConfiguracion(configuracion.id) {
                                        it.copy(menuAbierto = abierto)
                                    }
                                }
                            ) {
                                OutlinedTextField(
                                    value = configuracion.dado.etiqueta,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = !tirando,
                                    label = { Text("Tipo de dado") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(configuracion.menuAbierto)
                                    },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = configuracion.menuAbierto,
                                    onDismissRequest = {
                                        actualizarConfiguracion(configuracion.id) {
                                            it.copy(menuAbierto = false)
                                        }
                                    }
                                ) {
                                    opcionesDados.forEach { opcion ->
                                        DropdownMenuItem(
                                            text = { Text(opcion.etiqueta) },
                                            onClick = {
                                                actualizarConfiguracion(configuracion.id) {
                                                    it.copy(dado = opcion, menuAbierto = false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        configuraciones = configuraciones + ConfiguracionTirada(id = siguienteId)
                        siguienteId++
                    },
                    enabled = !tirando,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFE9C4))
                ) {
                    Text("Añadir tirada")
                }

                Button(
                    onClick = {
                        val cantidades = configuraciones.map { it.cantidad.toIntOrNull() }
                        when {
                            cantidades.any { it == null || it <= 0 } -> scope.launch {
                                snackbarHostState.showSnackbar("Introduce una cantidad válida en cada tirada")
                            }
                            cantidades.any { (it ?: 0) > LIMITE_DADOS } -> scope.launch {
                                snackbarHostState.showSnackbar("Máximo $LIMITE_DADOS dados por tirada")
                            }
                            else -> {
                                val configuracionesLanzadas = configuraciones.toList()
                                resultados = emptyList()
                                tirando = true
                                scope.launch {
                                    delay(DURACION_TIRADA_MS)
                                    resultados = configuracionesLanzadas.map { configuracion ->
                                        val cantidad = configuracion.cantidad.toInt()
                                        ResultadoTirada(
                                            configuracion = configuracion,
                                            valores = List(cantidad) {
                                                Random.nextInt(1, configuracion.dado.caras + 1)
                                            }
                                        )
                                    }
                                    tirando = false
                                }
                            }
                        }
                    },
                    enabled = !tirando,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = madera,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (tirando) "Lanzando..." else "Lanzar Dados")
                }

                if (resultados.isNotEmpty() && !tirando) {
                    val sumaDados = resultados.sumOf { it.valores.sum() }
                    val sumaModificadores = resultados.sumOf {
                        it.configuracion.modificador.toIntOrNull() ?: 0
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = pergamino),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "📜 Tiradas",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = madera
                            )
                            resultados.forEachIndexed { indice, resultado ->
                                val config = resultado.configuracion
                                val modificador = config.modificador.toIntOrNull() ?: 0
                                val textoModificador = if (modificador != 0) {
                                    " · ${formatearModificador(modificador)}"
                                } else ""
                                Text(
                                    "Tirada ${indice + 1}: ${config.cantidad}${config.dado.etiqueta}" +
                                        if (modificador != 0) " ${formatearModificador(modificador)}" else "",
                                    color = madera,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                resultado.valores.forEachIndexed { dadoIndex, valor ->
                                    val colorTexto = when {
                                        config.dado.caras != 20 -> Color.Black
                                        valor == 1 -> critFail
                                        valor == 20 -> critSuccess
                                        else -> Color.Black
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, madera, RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            "Dado ${dadoIndex + 1}: $valor",
                                            color = colorTexto,
                                            fontSize = 17.sp
                                        )
                                    }
                                }
                                Text(
                                    "Subtotal: ${resultado.valores.sum() + modificador} " +
                                        "(${resultado.valores.sum()} dados$textoModificador)",
                                    color = madera,
                                    fontSize = 16.sp
                                )
                                if (indice < resultados.lastIndex) {
                                    HorizontalDivider(color = madera.copy(alpha = 0.35f))
                                }
                            }
                            Text(
                                "Dados: $sumaDados  ·  Modificadores: ${formatearModificador(sumaModificadores)}",
                                color = madera,
                                fontSize = 17.sp
                            )
                            Text(
                                "🧮 Total: ${sumaDados + sumaModificadores}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = madera
                            )
                        }
                    }
                }
            }

            if (tirando) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF2180D08)),
                    contentAlignment = Alignment.Center
                ) {
                    Dado3DEscena(configuraciones = configuraciones, animando = true)
                }
            }
        }
    }
}

@Composable
private fun Dado3DEscena(
    configuraciones: List<ConfiguracionTirada>,
    animando: Boolean
) {
    val cantidadDados = configuraciones.sumOf { it.cantidad.toIntOrNull() ?: 1 }
        .coerceAtLeast(1)
        .coerceAtMost(MAX_DADOS_ANIMADOS)
    val carasDados = buildList {
        for (configuracion in configuraciones) {
            val restantes = MAX_DADOS_ANIMADOS - size
            if (restantes <= 0) break
            repeat((configuracion.cantidad.toIntOrNull() ?: 1).coerceAtLeast(1).coerceAtMost(restantes)) {
                add(configuracion.dado.caras)
            }
        }
    }
    var dadosFisicos by remember(configuraciones.map { it.id to it.cantidad }) {
        mutableStateOf(crearDadosFisicos(carasDados.ifEmpty { listOf(20) }))
    }

    LaunchedEffect(configuraciones, animando) {
        if (animando) {
            var ultimoTiempo = 0L
            val inicio = System.nanoTime()
            while (true) {
                val ahora = withFrameNanos { it }
                if (ultimoTiempo == 0L) ultimoTiempo = ahora
                val dt = min((ahora - ultimoTiempo) / 1_000_000_000f, 0.035f)
                ultimoTiempo = ahora
                val transcurrido = (ahora - inicio) / 1_000_000f
                dadosFisicos = actualizarFisica(dadosFisicos, dt)
                if (transcurrido >= DURACION_TIRADA_MS) break
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "🎲 Los dados están rodando…",
            color = Color(0xFFFFE9C4),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(Color(0xFF24140E), RoundedCornerShape(22.dp))
        ) {
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF4A2C1F), Color(0xFF1E110C))))
            drawRect(
                color = Color(0xFF17100D),
                topLeft = Offset(0f, size.height * 0.82f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.18f)
            )
            val escala = min(size.width, size.height)
            val tiempoAnimacion = System.nanoTime() / 1_000_000_000f
            dadosFisicos.take(cantidadDados).forEachIndexed { index, fisico ->
                val centro = Offset(fisico.x * size.width, fisico.y * size.height)
                val tam = escala * 0.13f
                drawCircle(Color.Black.copy(alpha = 0.38f), tam * 0.72f, Offset(centro.x + tam * 0.15f, centro.y + tam * 0.62f))
                dibujarDadoPoliedro3D(
                    caras = fisico.caras,
                    centro = centro,
                    tam = tam,
                    rotacion = fisico.rotacion + index * 17f,
                    tiempo = tiempoAnimacion
                )
            }
        }
        if (configuraciones.sumOf { it.cantidad.toIntOrNull() ?: 1 } > MAX_DADOS_ANIMADOS) {
            Text("Rodando ${cantidadDados} dados en la animación…", color = Color(0xFFFFE9C4))
        }
    }
}
private fun crearDadosFisicos(carasDados: List<Int>): List<DadoFisico> =
    carasDados.mapIndexed { index, caras ->
        val fila = index / 6
        val columna = index % 6
        DadoFisico(
            x = 0.12f + columna * 0.15f,
            y = 0.18f + fila * 0.10f,
            vx = Random.nextFloat() * 1.2f - 0.6f,
            vy = Random.nextFloat() * 0.8f - 0.2f,
            rotacion = Random.nextFloat() * 360f,
            velocidadRotacion = Random.nextFloat() * 600f - 300f,
            rebotes = 0,
            caras = caras
        )
    }

private fun actualizarFisica(
    dados: List<DadoFisico>,
    dt: Float
): List<DadoFisico> {
    val radio = 0.065f
    val gravedad = 1.15f

    val actualizados = dados.map { d ->
        var x = d.x + d.vx * dt
        var y = d.y + d.vy * dt
        var vx = d.vx
        var vy = d.vy + gravedad * dt
        var rebotes = d.rebotes

        if (x < radio) {
            x = radio
            vx = kotlin.math.abs(vx) * 0.82f
            rebotes++
        } else if (x > 1f - radio) {
            x = 1f - radio
            vx = -kotlin.math.abs(vx) * 0.82f
            rebotes++
        }

        if (y < radio) {
            y = radio
            vy = kotlin.math.abs(vy) * 0.80f
            rebotes++
        } else if (y > 0.78f - radio) {
            y = 0.78f - radio
            vy = -kotlin.math.abs(vy) * 0.76f
            vx *= 0.98f
            rebotes++
        }

        DadoFisico(
            x = x,
            y = y,
            vx = vx * 0.998f,
            vy = vy,
            rotacion = d.rotacion + d.velocidadRotacion * dt,
            velocidadRotacion = d.velocidadRotacion * 0.985f,
            rebotes = rebotes,
            caras = d.caras
        )
    }.toMutableList()

    // Colisiones sencillas entre dados: intercambio de velocidad aproximado.
    for (i in actualizados.indices) {
        for (j in i + 1 until actualizados.size) {
            val a = actualizados[i]
            val b = actualizados[j]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val distancia = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distancia in 0.001f..(radio * 2f)) {
                val nx = dx / distancia
                val ny = dy / distancia
                val relativa = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny

                if (relativa < 0f) {
                    actualizados[i] = a.copy(
                        vx = a.vx + nx * relativa * 0.9f,
                        vy = a.vy + ny * relativa * 0.9f
                    )
                    actualizados[j] = b.copy(
                        vx = b.vx - nx * relativa * 0.9f,
                        vy = b.vy - ny * relativa * 0.9f
                    )
                }
            }
        }
    }

    return actualizados
}

private data class Punto3D(val x: Float, val y: Float, val z: Float)

private val mallasDado: Map<Int, List<List<Punto3D>>> by lazy {
    listOf(4, 6, 8, 10, 12, 20, 100).associateWith(::crearMallaDado)
}

private fun mallaDado(caras: Int): List<List<Punto3D>> =
    mallasDado[caras] ?: crearMallaDado(caras)

private fun verticesIcosaedro(): List<Punto3D> {
    val phi = (1.0 + Math.sqrt(5.0)) / 2.0
    val p = phi.toFloat()
    return listOf(
        Punto3D(0f, -1f, -p), Punto3D(0f, -1f, p), Punto3D(0f, 1f, -p), Punto3D(0f, 1f, p),
        Punto3D(-1f, -p, 0f), Punto3D(-1f, p, 0f), Punto3D(1f, -p, 0f), Punto3D(1f, p, 0f),
        Punto3D(-p, 0f, -1f), Punto3D(p, 0f, -1f), Punto3D(-p, 0f, 1f), Punto3D(p, 0f, 1f)
    )
}

private fun carasIcosaedro(vertices: List<Punto3D>): List<List<Int>> {
    val caras = mutableListOf<List<Int>>()
    for (a in 0 until vertices.size) {
        for (b in a + 1 until vertices.size) {
            for (c in b + 1 until vertices.size) {
                val va = vertices[a]
                val vb = vertices[b]
                val vc = vertices[c]
                val ab = Punto3D(vb.x - va.x, vb.y - va.y, vb.z - va.z)
                val ac = Punto3D(vc.x - va.x, vc.y - va.y, vc.z - va.z)
                val normal = Punto3D(
                    ab.y * ac.z - ab.z * ac.y,
                    ab.z * ac.x - ab.x * ac.z,
                    ab.x * ac.y - ab.y * ac.x
                )
                val lados = vertices.indices.filter { it != a && it != b && it != c }.map { index ->
                    val v = vertices[index]
                    normal.x * (v.x - va.x) + normal.y * (v.y - va.y) + normal.z * (v.z - va.z)
                }
                if (lados.all { it >= -0.0001f } || lados.all { it <= 0.0001f }) {
                    caras += listOf(a, b, c)
                }
            }
        }
    }
    return caras
}

private fun mallaDodecaedro(): List<List<Punto3D>> {
    val vertices = verticesIcosaedro()
    val caras = carasIcosaedro(vertices)
    val centros = caras.map { indices ->
        val x = indices.sumOf { vertices[it].x.toDouble() } / 3.0
        val y = indices.sumOf { vertices[it].y.toDouble() } / 3.0
        val z = indices.sumOf { vertices[it].z.toDouble() } / 3.0
        val longitud = kotlin.math.sqrt(x * x + y * y + z * z)
        Punto3D((x / longitud).toFloat(), (y / longitud).toFloat(), (z / longitud).toFloat())
    }
    return vertices.mapIndexed { vertexIndex, eje ->
        val referencia = if (kotlin.math.abs(eje.z) < 0.9f) Punto3D(0f, 0f, 1f) else Punto3D(0f, 1f, 0f)
        val u = Punto3D(
            referencia.y * eje.z - referencia.z * eje.y,
            referencia.z * eje.x - referencia.x * eje.z,
            referencia.x * eje.y - referencia.y * eje.x
        )
        val longitudU = kotlin.math.sqrt(u.x * u.x + u.y * u.y + u.z * u.z)
        val unitariaU = Punto3D(u.x / longitudU, u.y / longitudU, u.z / longitudU)
        val v = Punto3D(
            eje.y * unitariaU.z - eje.z * unitariaU.y,
            eje.z * unitariaU.x - eje.x * unitariaU.z,
            eje.x * unitariaU.y - eje.y * unitariaU.x
        )
        caras.indices.filter { vertexIndex in caras[it] }
            .map { caraIndex ->
                val centro = centros[caraIndex]
                val angulo = kotlin.math.atan2(
                    centro.x * v.x + centro.y * v.y + centro.z * v.z,
                    centro.x * unitariaU.x + centro.y * unitariaU.y + centro.z * unitariaU.z
                )
                Pair(angulo, centros[caraIndex])
            }
            .sortedBy { it.first }
            .map { it.second }
    }
}

private fun mallaTrapezoedro(lados: Int = 5): List<List<Punto3D>> {
    val poloNorte = Punto3D(0f, 0f, 1.25f)
    val poloSur = Punto3D(0f, 0f, -1.25f)
    val anilloSuperior = (0 until lados).map { i ->
        val angle = 2.0 * Math.PI * i / lados
        Punto3D(cos(angle).toFloat(), sin(angle).toFloat(), 0.28f)
    }
    val anilloInferior = (0 until lados).map { i ->
        val angle = 2.0 * Math.PI * (i + 0.5) / lados
        Punto3D(cos(angle).toFloat(), sin(angle).toFloat(), -0.28f)
    }
    return (0 until lados).flatMap { i ->
        val siguiente = (i + 1) % lados
        listOf(
            listOf(poloNorte, anilloSuperior[i], anilloInferior[i], anilloSuperior[siguiente]),
            listOf(poloSur, anilloInferior[i], anilloSuperior[siguiente], anilloInferior[siguiente])
        )
    }
}

/** Modelos geométricos cerrados que se proyectan desde 3D a la escena Canvas. */
private fun crearMallaDado(caras: Int): List<List<Punto3D>> {
    val cube = listOf(
        Punto3D(-1f, -1f, -1f), Punto3D(1f, -1f, -1f),
        Punto3D(1f, 1f, -1f), Punto3D(-1f, 1f, -1f),
        Punto3D(-1f, -1f, 1f), Punto3D(1f, -1f, 1f),
        Punto3D(1f, 1f, 1f), Punto3D(-1f, 1f, 1f)
    )
    val cubeFaces = listOf(
        listOf(0, 1, 2, 3), listOf(4, 7, 6, 5), listOf(0, 4, 5, 1),
        listOf(3, 2, 6, 7), listOf(0, 3, 7, 4), listOf(1, 5, 6, 2)
    )
    fun faces(vertices: List<Punto3D>, indices: List<List<Int>>) = indices.map { face -> face.map(vertices::get) }
    fun prism(lados: Int): List<List<Punto3D>> {
        val vertices = (0 until lados).map { i ->
            val angle = i * 2.0 * Math.PI / lados
            Punto3D(cos(angle).toFloat(), sin(angle).toFloat(), 0.72f)
        } + (0 until lados).map { i ->
            val angle = i * 2.0 * Math.PI / lados
            Punto3D(cos(angle).toFloat(), sin(angle).toFloat(), -0.72f)
        }
        val indices = mutableListOf<List<Int>>()
        indices += (0 until lados).toList()
        indices += (lados until lados * 2).toList().reversed()
        for (i in 0 until lados) {
            val next = (i + 1) % lados
            indices += listOf(i, next, next + lados, i + lados)
        }
        return faces(vertices, indices)
    }

    return when (caras) {
        4 -> {
            val vertices = listOf(
                Punto3D(1f, 1f, 1f), Punto3D(1f, -1f, -1f),
                Punto3D(-1f, 1f, -1f), Punto3D(-1f, -1f, 1f)
            )
            faces(vertices, listOf(listOf(0, 1, 2), listOf(0, 1, 3), listOf(0, 2, 3), listOf(1, 2, 3)))
        }
        6 -> faces(cube, cubeFaces)
        8 -> {
            val vertices = listOf(
                Punto3D(0f, 0f, 1.25f), Punto3D(1f, 0f, 0f), Punto3D(0f, 1f, 0f),
                Punto3D(-1f, 0f, 0f), Punto3D(0f, -1f, 0f), Punto3D(0f, 0f, -1.25f)
            )
            val indices = listOf(
                listOf(0, 1, 2), listOf(0, 2, 3), listOf(0, 3, 4), listOf(0, 4, 1),
                listOf(5, 2, 1), listOf(5, 3, 2), listOf(5, 4, 3), listOf(5, 1, 4)
            )
            faces(vertices, indices)
        }
        10 -> mallaTrapezoedro()
        12 -> mallaDodecaedro()
        20 -> {
            val vertices = verticesIcosaedro()
            carasIcosaedro(vertices).map { cara -> cara.map { vertices[it] } }
        }
        100 -> mallaTrapezoedro()
        else -> faces(cube, cubeFaces)
    }
}

private fun DrawScope.dibujarDadoPoliedro3D(
    caras: Int,
    centro: Offset,
    tam: Float,
    rotacion: Float,
    tiempo: Float
) {
    val ax = tiempo * 2.3f + Math.toRadians(rotacion.toDouble()).toFloat()
    val ay = tiempo * 1.8f + Math.toRadians(rotacion * 0.73).toFloat()
    val az = tiempo * 2.7f
    fun transformar(p: Punto3D): Punto3D {
        val y1 = p.y * cos(ax) - p.z * sin(ax)
        val z1 = p.y * sin(ax) + p.z * cos(ax)
        val x2 = p.x * cos(ay) + z1 * sin(ay)
        val z2 = -p.x * sin(ay) + z1 * cos(ay)
        return Punto3D(x2 * cos(az) - y1 * sin(az), x2 * sin(az) + y1 * cos(az), z2)
    }
    val carasTransformadas = mallaDado(caras).map { cara ->
        val vertices = cara.map(::transformar)
        Triple(vertices, vertices.map { it.z }.average().toFloat(), vertices.map { vertice ->
            val perspectiva = tam * 2.8f / (4.2f - vertice.z)
            Offset(centro.x + vertice.x * perspectiva, centro.y - vertice.y * perspectiva)
        })
    }.sortedBy { it.second }
    val paleta = listOf(Color(0xFF76550B), Color(0xFFC9A227), Color(0xFFFFE082), Color(0xFF9B720F), Color(0xFFDAAF37))
    carasTransformadas.forEachIndexed { indice, (_, profundidad, puntos) ->
        drawPolygon(puntos.toTypedArray(), paleta[indice % paleta.size])
        if (profundidad > 0.08f) {
            val centroCara = Offset(puntos.map { it.x }.average().toFloat(), puntos.map { it.y }.average().toFloat())
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(72, 48, 10)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = tam * 0.62f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawContext.canvas.nativeCanvas.drawText(
                "✦", centroCara.x, centroCara.y - (paint.ascent() + paint.descent()) / 2f, paint
            )
        }
    }
}

private fun DrawScope.drawDado3D(
    dado: OpcionDado,
    resultado: Int,
    centro: Offset,
    tam: Float,
    rotacion: Float
) {
    val rad = Math.toRadians(rotacion.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()

    fun punto(x: Float, y: Float): Offset =
        Offset(
            centro.x + (x * c - y * s) * tam,
            centro.y + (x * s + y * c) * tam
        )

    val base = Color(0xFFC9A227)
    val oscuro = Color(0xFF76550B)
    val claro = Color(0xFFFFE082)
    val tinta = Color(0xFF24170A)

    when (dado.caras) {
        4 -> {
            val arriba = punto(0f, -1f)
            val izq = punto(-0.9f, 0.75f)
            val abajo = punto(0f, 0.95f)
            val der = punto(0.9f, 0.75f)

            drawPath(Path().apply {
                moveTo(arriba.x, arriba.y); lineTo(izq.x, izq.y)
                lineTo(abajo.x, abajo.y); close()
            }, oscuro)
            drawPath(Path().apply {
                moveTo(arriba.x, arriba.y); lineTo(der.x, der.y)
                lineTo(abajo.x, abajo.y); close()
            }, base)
            dibujarNumero(centro, resultado, tam, tinta)
        }

        6 -> {
            val frente = arrayOf(punto(-0.75f, -0.65f), punto(0.65f, -0.65f), punto(0.75f, 0.65f), punto(-0.65f, 0.65f))
            val arriba = arrayOf(punto(-0.75f, -0.65f), punto(-0.25f, -0.95f), punto(0.95f, -0.75f), punto(0.65f, -0.65f))
            val lado = arrayOf(punto(0.65f, -0.65f), punto(0.95f, -0.75f), punto(0.95f, 0.45f), punto(0.75f, 0.65f))

            drawPolygon(frente, base)
            drawPolygon(arriba, claro)
            drawPolygon(lado, oscuro)
            dibujarNumero(centro, resultado, tam, tinta)
        }

        8 -> {
            val arriba = punto(0f, -1f)
            val izq = punto(-0.95f, 0f)
            val centroBajo = punto(0f, 0.25f)
            val der = punto(0.95f, 0f)
            val abajo = punto(0f, 1f)

            drawPolygon(arrayOf(arriba, izq, centroBajo), claro)
            drawPolygon(arrayOf(arriba, centroBajo, der), base)
            drawPolygon(arrayOf(izq, abajo, centroBajo), oscuro)
            drawPolygon(arrayOf(centroBajo, abajo, der), Color(0xFF9B720F))
            dibujarNumero(centro, resultado, tam, tinta)
        }

        10 -> dibujarDadoFacetas(5, base, claro, oscuro, centro, tam, rotacion, resultado, tinta)
        12 -> dibujarDadoFacetas(6, base, claro, oscuro, centro, tam, rotacion, resultado, tinta)
        20 -> dibujarDadoFacetas(8, base, claro, oscuro, centro, tam, rotacion, resultado, tinta)
        100 -> {
            // El D100 se representa como un dado porcentual esférico facetado.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(claro, base, oscuro),
                    center = centro,
                    radius = tam * 1.05f
                ),
                radius = tam * 0.92f,
                center = centro
            )
            drawCircle(
                color = Color(0xFF5B410A),
                radius = tam * 0.92f,
                center = centro,
                style = Stroke(width = tam * 0.07f)
            )
            for (i in 1..4) {
                val angulo = rotacion + i * 36f
                val rr = Math.toRadians(angulo.toDouble())
                val p1 = Offset(
                    centro.x + cos(rr).toFloat() * tam * 0.84f,
                    centro.y + sin(rr).toFloat() * tam * 0.84f
                )
                val p2 = Offset(
                    centro.x - cos(rr).toFloat() * tam * 0.84f,
                    centro.y - sin(rr).toFloat() * tam * 0.84f
                )
                drawLine(Color(0x55906B10), p1, p2, tam * 0.025f)
            }
            dibujarNumero(centro, resultado, tam, tinta)
        }
    }
}

private fun DrawScope.dibujarDadoFacetas(
    facetas: Int,
    base: Color,
    claro: Color,
    oscuro: Color,
    centro: Offset,
    tam: Float,
    rotacion: Float,
    resultado: Int,
    tinta: Color
) {
    val rad = Math.toRadians(rotacion.toDouble())
    val anguloBase = rad.toFloat()
    val vertices = (0 until facetas).map { i ->
        val a = anguloBase + i * (Math.PI * 2 / facetas).toFloat()
        Offset(
            centro.x + cos(a).toFloat() * tam,
            centro.y + sin(a).toFloat() * tam * 0.82f
        )
    }

    for (i in 0 until facetas) {
        val next = (i + 1) % facetas
        val color = when (i % 3) {
            0 -> claro
            1 -> base
            else -> oscuro
        }
        drawPolygon(arrayOf(centro, vertices[i], vertices[next]), color)
    }

    drawCircle(
        color = Color(0xFF5B410A),
        radius = tam,
        center = centro,
        style = Stroke(width = tam * 0.045f)
    )
    dibujarNumero(centro, resultado, tam, tinta)
}

private fun DrawScope.drawPolygon(vertices: Array<Offset>, color: Color) {
    val path = Path()
    path.moveTo(vertices[0].x, vertices[0].y)
    for (i in 1 until vertices.size) path.lineTo(vertices[i].x, vertices[i].y)
    path.close()
    drawPath(path, color, style = Fill)
    drawPath(path, Color(0x66452F05), style = Stroke(width = 2f))
}

private fun DrawScope.dibujarNumero(
    centro: Offset,
    numero: Int,
    tam: Float,
    color: Color
) {
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgbCompat()
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = tam * if (numero >= 100) 0.48f else 0.58f
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
    }

    drawContext.canvas.nativeCanvas.drawText(
        numero.toString(),
        centro.x,
        centro.y - (paint.ascent() + paint.descent()) / 2f,
        paint
    )
}

private fun Color.toArgbCompat(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )

private fun formatearModificador(modificador: Int): String =
    if (modificador > 0) "+$modificador" else modificador.toString()

