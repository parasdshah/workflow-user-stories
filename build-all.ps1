Write-Host "Starting Build Process for All Projects..."

# Stop services if script exists
if (Test-Path ".\stop-all.ps1") {
    Write-Host "Stopping running services..."
    .\stop-all.ps1
}

$ErrorActionPreference = "Stop"

function Run-Build($path, $command, $argsList) {
    Write-Host "Building in $path..."
    Push-Location $path
    try {
        # Directly execute the command. Powershell handles standard streams better this way than Start-Process for this case.
        # We assume 'mvn' and 'npm' are in PATH.
        & $command $argsList.Split(" ")
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Build failed in $path"
            exit 1
        }
    } finally {
        Pop-Location
    }
}

# 1. Service Registry
Run-Build "service-registry" "mvn" "clean install -DskipTests"

# 2. API Gateway
Run-Build "api-gateway" "mvn" "clean install -DskipTests"

# 3. Workflow Service
Run-Build "workflow-delegates" "mvn" "clean install -DskipTests"
Run-Build "workflow-service" "mvn" "clean install -DskipTests"

# 4. Frontend
Write-Host "Building dependencies and project in workflow-ui..."
Push-Location workflow-ui
try {
    # Install
    Write-Host "Running npm install..."
    npm install
    if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
    
    # Build
    Write-Host "Running npm run build..."
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build failed" }
} finally {
    Pop-Location
}

Write-Host "All projects built successfully!" -ForegroundColor Green
