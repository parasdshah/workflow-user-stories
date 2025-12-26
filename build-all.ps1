Write-Host "Starting Build Process for All Projects..."
$ErrorActionPreference = "Stop"

function Run-Build($path, $command, $argsList) {
    Write-Host "Building in $path..."
    Push-Location $path
    try {
        Start-Process -FilePath $command -ArgumentList $argsList -Wait -NoNewWindow -PassThru | ForEach-Object {
            if ($_.ExitCode -ne 0) {
                Write-Error "Build failed in $path with exit code $($_.ExitCode)"
                exit 1
            }
        }
    }
    finally {
        Pop-Location
    }
}

# 1. Service Registry
Run-Build "service-registry" "mvn" "clean install -DskipTests"

# 2. API Gateway
Run-Build "api-gateway" "mvn" "clean install -DskipTests"

# 3. Workflow Service
Run-Build "workflow-service" "mvn" "clean install -DskipTests"

# 4. Frontend
Write-Host "Building dependencies and project in workflow-ui..."
Push-Location workflow-ui
try {
    # Install
    Write-Host "Running npm install..."
    Start-Process -FilePath "npm.cmd" -ArgumentList "install" -Wait -NoNewWindow -PassThru | ForEach-Object {
        if ($_.ExitCode -ne 0) {
            throw "npm install failed"
        }
    }
    
    # Build
    Write-Host "Running npm run build..."
    Start-Process -FilePath "npm.cmd" -ArgumentList "run build" -Wait -NoNewWindow -PassThru | ForEach-Object {
        if ($_.ExitCode -ne 0) {
            throw "npm run build failed"
        }
    }
}
catch {
    Write-Error "Frontend build failed: $_"
    exit 1
}
finally {
    Pop-Location
}

Write-Host "All projects built successfully!" -ForegroundColor Green
