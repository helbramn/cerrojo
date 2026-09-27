# Cerrojo

Bloqueo de apps por tiempo de uso para Android, con límites que se calculan
a partir del uso real y bajan un 10 % cada semana. Incluye dentro la app de
disciplina.

## Instalar en el móvil

1. Abre la última release desde el móvil: https://github.com/helbramn/cerrojo/releases/latest
2. Descarga el `.apk` y ábrelo. Android pedirá permiso para instalar de esta fuente.
3. Al abrir la app por primera vez, pasa los cinco permisos de MIUI que te pide.

### Actualizar

Desde la v0.6 todas las releases van firmadas con la misma clave, así que las
siguientes se instalan encima sin perder nada.

**Solo una vez**: para pasar de una versión anterior a la v0.6 hay que
desinstalar primero. Las versiones hasta la v0.5 se firmaban con una clave
distinta en cada compilación y Android rechaza la actualización. Desinstalar
borra los límites, las apps vigiladas y la fecha de instalación que cuenta las
semanas: el cerrojo vuelve a medir catorce días desde cero.

## Desarrollo

No hace falta Android Studio ni el SDK: todo se compila en GitHub Actions.
`./gradlew :core:test` corre los tests del motor; el resto se verifica en el móvil.

## Documentación

- [`docs/errores-y-arreglos.md`](docs/errores-y-arreglos.md) — todos los fallos
  que ha tenido esto y cómo se arreglaron.
- [Historial de versiones](https://github.com/helbramn/cerrojo/releases) — una
  release por versión, con su APK. Una versión publicada no se vuelve a tocar.
