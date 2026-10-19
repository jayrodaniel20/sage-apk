SAGE APK DE INSTALACIÓN ALTERNATIVA
==================================

Objetivo:
Este proyecto crea un APK instalable para celulares Android donde Chrome no muestra
"Instalar aplicación" ni "Agregar a pantalla principal".

La aplicación abre siempre la versión actual de SAGE publicada en:
https://jayrodaniel20.github.io/registro-ocurrencias/

Por eso, cuando actualices SAGE en GitHub Pages normalmente NO tendrás que volver
a instalar el APK. El APK actúa como acceso nativo a la versión web actual.

Nombre de la app: SAGE
Paquete Android: pe.sage.app
Versión del contenedor: 1.0
Android mínimo: Android 8.0 (API 26)

FUNCIONES INCLUIDAS EN EL CONTENEDOR
- Inicio de sesión y Supabase.
- Almacenamiento web/local.
- Selector de archivos para importar Excel/CSV.
- Botón Atrás dentro de SAGE.
- Acceso a micrófono y puente de reconocimiento de voz si WebView no expone Web Speech API.
- Ícono oficial de SAGE.

CÓMO GENERAR EL APK SIN ANDROID STUDIO
1. Crea un repositorio nuevo en GitHub.
2. Sube TODO el contenido de esta carpeta conservando .github/workflows/build-apk.yml.
3. GitHub ejecutará automáticamente la acción "Build SAGE APK".
4. En GitHub entra a Actions > Build SAGE APK > ejecución más reciente.
5. Al final descarga el artefacto "SAGE-APK".
6. Dentro encontrarás SAGE.apk.
7. Pásalo al celular e instálalo permitiendo temporalmente "Instalar apps desconocidas".

IMPORTANTE
Este APK es un método alternativo para el piloto. Conviene probar en 1 o 2 equipos
antes de distribuirlo a toda la institución, especialmente dictado por voz,
importación de Excel/CSV y generación de reportes.
