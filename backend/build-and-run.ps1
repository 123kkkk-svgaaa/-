$env:JAVA_HOME = "C:\Users\17619\.jdks\ms-21.0.9"
$env:MAVEN_HOME = "C:\Users\17619\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

Write-Host "Java:" (java -version 2>&1 | Select-Object -First 1)
Write-Host "Maven:" (mvn -version 2>&1 | Select-Object -First 1)

# Use current script directory to locate backend
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "Working dir: $PWD"
Write-Host "Building backend..."

mvn clean package -DskipTests -q 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Build OK! Starting..."
    $jar = Get-ChildItem target\dapp-voting-backend-*.jar | Select-Object -First 1
    java -jar $jar.FullName
} else {
    Write-Host "Build FAILED: $LASTEXITCODE"
    exit 1
}
