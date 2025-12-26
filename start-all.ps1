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
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -f workflow-service/pom.xml" -NoNewWindow
Start-Sleep -Seconds 15

# Start Frontend
Write-Host "Starting Frontend..."
Set-Location workflow-ui
Start-Process -FilePath "npm.cmd" -ArgumentList "run dev" -NoNewWindow
