Write-Host "Stopping all workflow services..."

# Stop Backend Services (Java)
try {
    $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
    if ($javaProcesses) {
        Write-Host "Stopping Java processes (Backend)..."
        $javaProcesses | Stop-Process -Force
        Write-Host "Java processes stopped."
    }
    else {
        Write-Host "No Java processes found."
    }
}
catch {
    Write-Host "Error stopping Java processes: $_"
}

# Stop Frontend (Node.js)
try {
    $nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
    if ($nodeProcesses) {
        Write-Host "Stopping Node.js processes (Frontend)..."
        $nodeProcesses | Stop-Process -Force
        Write-Host "Node processes stopped."
    }
    else {
        Write-Host "No Node processes found."
    }
}
catch {
    Write-Host "Error stopping Node processes: $_"
}

Write-Host "All services stopped."
