# Cerrojo

App Android para bloquear otras apps por tiempo de uso, con la web de disciplina
(`app-disciplina`) embebida en un WebView.

## Módulos

- `:core` — lógica pura en Kotlin (JVM), sin dependencias de Android.
- `:app` — app Android.

## Desarrollo

Requiere JDK 17. Sin Gradle instalado, usa el wrapper:

```bash
./gradlew :core:test :app:assembleDebug
```
