# Stop services if script exists
if (Test-Path ".\stop-all.ps1") {
    Write-Host "Stopping running services..."
    .\stop-all.ps1
}


# Start Service Registry
Write-Host "Starting Service Registry..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -f service-registry/pom.xml" -NoNewWindow
Start-Sleep -Seconds 15

# Start API Gateway
Write-Host "Starting API Gateway..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -f api-gateway/pom.xml" -NoNewWindow
Start-Sleep -Seconds 10

# Start Workflow Service
Write-Host "Starting Workflow Service..."
Start-Process -FilePath "java" -ArgumentList "-Dloader.path=workflow-delegates/target/workflow-delegates-1.0.0-SNAPSHOT.jar -jar workflow-service/target/workflow-service-1.0.0-SNAPSHOT.jar" -NoNewWindow
Start-Sleep -Seconds 15

# Start HRMS Service
Write-Host "Starting HRMS Service..."
Start-Process -FilePath "java" -ArgumentList "-jar hrms-service/target/hrms-service-1.0.0-SNAPSHOT.jar" -NoNewWindow
Start-Sleep -Seconds 10

# Start Frontend
Write-Host "Starting Frontend..."
Set-Location workflow-ui
Start-Process -FilePath "npm.cmd" -ArgumentList "run dev" -NoNewWindow
