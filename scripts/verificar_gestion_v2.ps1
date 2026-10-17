param(
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$RepositorioMaven = "$env:USERPROFILE\.m2\repository",
    [switch]$ConBase
)

$ErrorActionPreference = 'Stop'
$raizGestion = Split-Path -Parent $PSScriptRoot
Push-Location $raizGestion
try {
    $salidaGestion = 'target/gestion-v2-check'
    New-Item -ItemType Directory -Force -Path $salidaGestion | Out-Null
    $jsonGestion = Join-Path $RepositorioMaven 'com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar'
    $jdbcGestion = Join-Path $RepositorioMaven 'org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar'
    $dependenciasGestion = "$jsonGestion;$jdbcGestion"
    $baseGestion = 'src/main/java/fpuna/supermax'
    $fuentesGestion = @(
        "$baseGestion/entidad/Reserva.java",
        "$baseGestion/entidad/ReservaItem.java",
        "$baseGestion/entidad/ReservaException.java",
        "$baseGestion/bd/Bd.java",
        "$baseGestion/bd/GestionReservaConsultas.java",
        "$baseGestion/bd/GestionReservaDAO.java",
        "$baseGestion/servicio/GestionReservaServicio.java"
    )
    $fuentesGestion += Get-ChildItem 'src/test/java/fpuna/supermax/bd/GestionReserva*.java' |
        Select-Object -ExpandProperty FullName
    $diagnosticoGestion = Join-Path $salidaGestion 'compilacion.log'
    $ErrorActionPreference = 'Continue'
    try {
        & "$JdkHome/bin/javac.exe" --release 8 -encoding UTF-8 -cp $dependenciasGestion -d $salidaGestion @fuentesGestion 2> $diagnosticoGestion
        $codigoGestion = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = 'Stop'
    }
    Get-Content $diagnosticoGestion | Write-Host
    $falloCompilador = Select-String -Path $diagnosticoGestion -Pattern 'An exception has occurred in the compiler|AccessDeniedException'
    if ($codigoGestion -ne 0 -or $falloCompilador) {
        throw 'Fallo la compilacion del modulo de gestion; revisar target/gestion-v2-check/compilacion.log'
    }
    & "$JdkHome/bin/java.exe" -cp "$salidaGestion;$dependenciasGestion" fpuna.supermax.bd.GestionReservaValidacion
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de validacion' }
    if ($ConBase) {
        & "$JdkHome/bin/java.exe" -cp "$salidaGestion;$dependenciasGestion" fpuna.supermax.bd.GestionReservaIntegracion
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de PostgreSQL' }
    }
} finally {
    Pop-Location
}
