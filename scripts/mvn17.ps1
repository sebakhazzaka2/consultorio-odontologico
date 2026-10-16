# Corre Maven con JDK 17 (el de CI y producción) sin tocar el JAVA_HOME global.
# Uso: .\scripts\mvn17.ps1 test        (desde la raíz del repo)
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" |
    Sort-Object Name -Descending | Select-Object -First 1
if (-not $jdk) {
    Write-Error "JDK 17 no encontrado. Instalar: winget install EclipseAdoptium.Temurin.17.JDK"
    exit 1
}
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($jdk.FullName)\bin;$env:Path"
Push-Location (Join-Path $PSScriptRoot "..\backend")
try { mvn @args; exit $LASTEXITCODE } finally { Pop-Location }
