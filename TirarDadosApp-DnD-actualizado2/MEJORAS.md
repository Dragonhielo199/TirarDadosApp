# Mejoras realizadas

## D100
- Se ha añadido `D100` al selector.
- `TipoDado` incluye ahora `D100(100)`.
- La tirada usa `Random.nextInt(1, 101)`, por lo que puede devolver 1..100.
- El D100 tiene una representación visual propia en la escena.

## Dados animados
- Las tiradas se generan **antes** de comenzar la animación.
- Se muestran tantos dados animados como resultados obtenidos.
- Se ha establecido un límite de 30 dados para evitar sobrecargar la escena.
- Cada dado conserva y muestra su resultado real durante la animación.
- La escena utiliza Canvas de Compose y no necesita una librería 3D externa.
- La animación incluye gravedad, rebotes contra los bordes de la mesa, giro y colisiones aproximadas entre dados.
- Los dados tienen un render facetado con sombreado para dar sensación de volumen.

> Nota: esta implementación es un render 3D simulado mediante Canvas/software rendering, no un modelo 3D de Filament/OpenGL con malla física real. Se ha elegido así para mantener el proyecto ligero y evitar añadir dependencias externas.

## Resultado y navegación
- La pantalla principal ahora es desplazable verticalmente.
- Se respetan los `WindowInsets` de la pantalla y la barra de navegación.
- Se ha eliminado la lista anidada que podía quedarse limitada a una altura fija.
- El total se muestra con un tamaño mayor y nunca debería quedar oculto detrás de la barra inferior del sistema.

## Lógica Java
- `ClasesDeDados` valida cantidades y número de caras.
- `TipoDado` incluye el nuevo D100.

## Nota sobre la compilación
El proyecto no se ha podido compilar dentro de este entorno porque Gradle intentó descargar `gradle-9.2.1` desde Internet y el entorno no permite conexiones de red. El código ha sido revisado estáticamente después de los cambios.
