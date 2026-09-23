# Cerrojo

Bloqueo de apps por tiempo de uso para Android, con límites que se calculan
a partir del uso real y bajan un 10 % cada semana. Incluye dentro la app de
disciplina.

## Instalar en el móvil

1. Abre la última release desde el móvil: https://github.com/helbramn/cerrojo/releases/latest
2. Descarga el `.apk` y ábrelo. Android pedirá permiso para instalar de esta fuente.
3. Al abrir la app por primera vez, pasa los cinco permisos de MIUI que te pide.

## Desarrollo

No hace falta Android Studio ni el SDK: todo se compila en GitHub Actions.
`./gradlew :core:test` corre los tests del motor; el resto se verifica en el móvil.
