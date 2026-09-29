$ErrorActionPreference = 'Stop'
$env:SMART_CLOSET_DB_USER = 'SMART_CLOSET'
$securePassword = Read-Host 'SMART_CLOSET Oracle password' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:SMART_CLOSET_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}

try {
    & "$PSScriptRoot\..\gradlew.bat" -p "$PSScriptRoot" run --console=plain
} finally {
    Remove-Item Env:SMART_CLOSET_DB_PASSWORD -ErrorAction SilentlyContinue
}
