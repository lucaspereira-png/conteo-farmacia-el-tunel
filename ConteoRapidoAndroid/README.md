# Conteo Rápido — proyecto Android para uso personal

Este proyecto envuelve la versión original de `Conteo Rápido` en una aplicación Android local. No se publica en Google Play ni necesita servidor.

## Preparación
- Se integró el HTML original `Conteo_rapido_v2_2(1).html` sin cambiar la lógica JavaScript del conteo.
- Se añadió `inputmode="none"` únicamente al campo del escáner para solicitar que Android no muestre el teclado virtual. El lector físico debe probarse en el equipo real.
- La WebView usa `WebViewAssetLoader` para cargar los recursos incluidos en la aplicación.
- El selector de archivos de Android permite importar la matriz.
- Las exportaciones creadas como Blob se redirigen al selector nativo de guardado de Android.
- El conteo usa el almacenamiento local del navegador/WebView. No desinstales la aplicación ni borres sus datos antes de probar recuperación/respaldo.

## Cómo compilar el APK
1. Instalar Android Studio desde https://developer.android.com/studio.
2. Descomprimir el ZIP.
3. En Android Studio, elegir **Open** y seleccionar la carpeta `ConteoRapidoAndroid`.
4. Aceptar la instalación del Android SDK que solicite y esperar la sincronización de Gradle.
5. Seleccionar **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Al finalizar, Android Studio muestra la ubicación del APK (normalmente `app/build/outputs/apk/debug/app-debug.apk`).
7. Copiar el APK al dispositivo Android e instalarlo. Si Android lo pide, habilitar temporalmente la instalación desde esa fuente.

## Pruebas pendientes en el dispositivo
Este entorno no dispone de Android SDK/Gradle y no puede generar el APK binario ni ejecutar la aplicación en un lector físico. Por eso, el APK no se declara compilado ni probado.

Antes de usar en inventario real, comprobar:
1. Importar una matriz Excel de prueba.
2. Escanear un código conocido dos veces y verificar que suma dos veces la cantidad configurada.
3. Escanear un código que no exista y comprobar que aparezca en “Sin coincidencia”.
4. Cambiar la presentación y confirmar el multiplicador.
5. Exportar Conteo a XLSX y códigos sin coincidencia; guardar y abrir los archivos.
6. Exportar CSV y abrirlo.
7. Registrar algunas unidades, cerrar y volver a abrir la aplicación; revisar recuperación/respaldo.
8. Confirmar que el lector funciona con teclado virtual oculto. Si el dispositivo no escribe en el campo, tocar el campo de escaneo una vez y repetir la prueba.

## Configuración
- Paquete: `com.eltunel.conteorapido`
- Nombre visible: `Conteo Rápido`
- Versión: `1.0.0`

## Compilar sin Android Studio (GitHub Actions)
1. Crear un repositorio en GitHub y subir todo el contenido de esta carpeta (incluida `.github/`).
2. Ir a la pestaña **Actions**; el workflow **Build APK** corre solo (o con "Run workflow").
3. Al terminar, descargar el artefacto **ConteoRapido-APK** (contiene `app-debug.apk`).
