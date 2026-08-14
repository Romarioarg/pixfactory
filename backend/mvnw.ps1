$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Props = Get-Content (Join-Path $Root ".mvn\wrapper\maven-wrapper.properties")
$Url = ($Props | Where-Object { $_ -like "distributionUrl=*" }) -replace "distributionUrl=", ""
$HomeDir = if ($env:USERPROFILE) { $env:USERPROFILE } else { $env:HOME }
$DistDir = Join-Path $HomeDir ".m2\wrapper\dists\apache-maven-3.9.9"
$Mvn = Get-ChildItem -Path $DistDir -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $Mvn) {
  New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
  $Zip = Join-Path $DistDir "maven.zip"
  Write-Host "Downloading Maven 3.9.9..."
  Invoke-WebRequest -Uri $Url -OutFile $Zip
  Expand-Archive -Path $Zip -DestinationPath $DistDir -Force
  $Mvn = Get-ChildItem -Path $DistDir -Filter "mvn.cmd" -Recurse | Select-Object -First 1
}
if (-not $env:JAVA_HOME -and (Test-Path "C:\Program Files\Java\jdk-24")) {
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
}
& $Mvn.FullName @args
exit $LASTEXITCODE
