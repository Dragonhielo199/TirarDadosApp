# TirarDadosApp

Aplicación Android para lanzar dados de rol. Permite configurar varias tiradas independientes y ejecutarlas juntas, con modificadores y una animación central antes de mostrar los resultados.

## Funciones

- Dados D4, D6, D8, D10, D12, D20 y D100.
- Hasta 100 dados por cada tirada configurada.
- Un tipo de dado, una cantidad y un modificador propios para cada tirada.
- Botón **Añadir tirada** para agregar configuraciones y **Lanzar Dados** para ejecutarlas juntas.
- Resultados detallados por tirada y por dado, con subtotales y total general.
- La animación cubre la pantalla durante el lanzamiento. Los valores se generan al terminar, así que no se muestran antes.

La animación dibuja geometría con coordenadas 3D, rotaciones en tres ejes y proyección en perspectiva mediante Canvas de Jetpack Compose. La posición y los rebotes se calculan en el plano de la mesa. Para mantener fluida la escena, se animan visualmente hasta 24 dados; la tirada y sus resultados pueden incluir hasta 100 dados por configuración.

## Cómo se calcula una tirada

Cada configuración valida que la cantidad esté entre 1 y 100. Al terminar la animación, la aplicación genera cada resultado entre 1 y el número de caras del dado. El total general es la suma de todos los resultados más la suma de los modificadores.

Por ejemplo, se pueden configurar `4d6 + 2` y `8d8 - 1`; el botón **Lanzar Dados** ejecuta ambas configuraciones y muestra cada lista de resultados y el total combinado. Al iniciar una nueva tirada, los resultados anteriores se reemplazan.

## Organización del código

### `MainActivity.kt`

Contiene la pantalla principal y la lógica usada por la interfaz:

- `PantallaPrincipal()` construye los controles, mantiene las configuraciones y valida las cantidades.
- `ConfiguracionTirada` guarda el tipo de dado, la cantidad y el modificador de cada grupo.
- `ResultadoTirada` conserva los resultados de un grupo para mostrarlos en una lista.
- `Dado3DEscena()` dibuja la animación superpuesta a la pantalla.
- Las funciones de malla y proyección representan las formas de los dados en perspectiva.
- La lógica de lanzamiento genera los valores después de la animación y suma dados y modificadores.

### Clases auxiliares Java

- `TipoDado.java` enumera los dados disponibles y su número de caras.
- `ClasesDeDados.java` contiene el método auxiliar `tirarDados(caras, cantidad)`, que valida los parámetros y devuelve una lista de valores con su suma. La pantalla Compose realiza su lanzamiento directamente en Kotlin.

### Recursos

`app/src/main/res/` contiene los recursos visuales, iconos, colores, temas y textos de la aplicación.

## Tecnologías

- Kotlin y Java
- Android y Jetpack Compose
- Material 3
- Canvas de Compose para la escena animada

## Requisitos

- Android Studio
- JDK 17 o posterior
- Android SDK Platform 35
- Android 7.0 (API 24) o posterior para ejecutar la aplicación

El proyecto usa Gradle 8.9, Android Gradle Plugin 8.7.3 y Kotlin 2.0.21.

## Abrir y compilar

Abre en Android Studio la carpeta del proyecto que contiene `settings.gradle.kts` y espera a que termine la sincronización de Gradle. Si falta Android SDK Platform 35, instálalo desde el SDK Manager.

Para compilar desde una terminal:

```powershell
# Windows
.\gradlew.bat :app:assembleDebug
```

```bash
# macOS o Linux
./gradlew :app:assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Android Studio crea `local.properties` para indicar la ubicación del SDK en cada ordenador; ese archivo no debe compartirse.
