$files = Get-ChildItem .\src\main\java -Recurse -Filter *.java
foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Output "Removing UTF8 BOM: $($f.FullName)"
        $text = [System.Text.Encoding]::UTF8.GetString($bytes,3,$bytes.Length-3)
        [System.IO.File]::WriteAllBytes($f.FullName, [System.Text.Encoding]::UTF8.GetBytes($text))
    } elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xFE) {
        Write-Output "Converting UTF-16 LE -> UTF8: $($f.FullName)"
        $text = [System.Text.Encoding]::Unicode.GetString($bytes)
        [System.IO.File]::WriteAllBytes($f.FullName, [System.Text.Encoding]::UTF8.GetBytes($text))
    } elseif ($bytes.Length -ge 2 -and $bytes[0] -eq 0xFE -and $bytes[1] -eq 0xFF) {
        Write-Output "Converting UTF-16 BE -> UTF8: $($f.FullName)"
        $text = [System.Text.Encoding]::BigEndianUnicode.GetString($bytes)
        [System.IO.File]::WriteAllBytes($f.FullName, [System.Text.Encoding]::UTF8.GetBytes($text))
    }
}
Write-Output "Done."