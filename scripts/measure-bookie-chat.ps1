param(
    [Parameter(Mandatory = $true)]
    [string]$Token,

    [Parameter(Mandatory = $false)]
    [string]$BaseUrl = "http://localhost:8080",

    [Parameter(Mandatory = $false)]
    [string]$Message = "힐링되는 소설 추천해줘",

    [Parameter(Mandatory = $false)]
    [int]$Iterations = 3
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

$jsonBody = @{ message = $Message } | ConvertTo-Json -Compress
$headers = @{
    Authorization = "Bearer $Token"
    Accept = "application/json"
}
$streamHeaders = @{
    Authorization = "Bearer $Token"
    Accept = "text/event-stream"
}

function Invoke-JsonChat {
    param(
        [string]$Url,
        [hashtable]$Headers,
        [string]$Body
    )

    $client = [System.Net.Http.HttpClient]::new()
    try {
        foreach ($key in $Headers.Keys) {
            if ($key -ieq "Accept") {
                $client.DefaultRequestHeaders.Accept.Clear()
                $client.DefaultRequestHeaders.Accept.Add(
                    [System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::new($Headers[$key])
                )
            } else {
                $client.DefaultRequestHeaders.Remove($key) | Out-Null
                $client.DefaultRequestHeaders.Add($key, $Headers[$key])
            }
        }

        $content = [System.Net.Http.StringContent]::new($Body, [System.Text.Encoding]::UTF8, "application/json")
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $response = $client.PostAsync($Url, $content).GetAwaiter().GetResult()
        $payload = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $sw.Stop()

        [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            ElapsedMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
            Body = $payload
        }
    }
    finally {
        $client.Dispose()
    }
}

function Invoke-SseChat {
    param(
        [string]$Url,
        [hashtable]$Headers,
        [string]$Body
    )

    $handler = [System.Net.Http.HttpClientHandler]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)

    try {
        foreach ($key in $Headers.Keys) {
            if ($key -ieq "Accept") {
                $client.DefaultRequestHeaders.Accept.Clear()
                $client.DefaultRequestHeaders.Accept.Add(
                    [System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::new($Headers[$key])
                )
            } else {
                $client.DefaultRequestHeaders.Remove($key) | Out-Null
                $client.DefaultRequestHeaders.Add($key, $Headers[$key])
            }
        }

        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, $Url)
        $request.Content = [System.Net.Http.StringContent]::new($Body, [System.Text.Encoding]::UTF8, "application/json")

        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $response = $client.SendAsync(
            $request,
            [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead
        ).GetAwaiter().GetResult()

        $stream = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
        $reader = [System.IO.StreamReader]::new($stream)

        $firstEventMs = $null
        $lineCount = 0
        $events = New-Object System.Collections.Generic.List[string]

        try {
            while (-not $reader.EndOfStream) {
                $line = $reader.ReadLine()
                if ($null -eq $line) {
                    continue
                }

                if ($line.Length -gt 0 -and -not $firstEventMs) {
                    $firstEventMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                }

                if ($line.StartsWith("event:")) {
                    $events.Add($line.Substring(6).Trim())
                }

                $lineCount++
            }
        }
        finally {
            $reader.Dispose()
            $stream.Dispose()
            $response.Dispose()
        }

        $sw.Stop()

        [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            FirstEventMs = $firstEventMs
            TotalMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
            LineCount = $lineCount
            Events = ($events -join ",")
        }
    }
    finally {
        $client.Dispose()
        $handler.Dispose()
    }
}

$jsonResults = @()
$sseResults = @()

for ($i = 1; $i -le $Iterations; $i++) {
    $jsonResults += Invoke-JsonChat -Url "$BaseUrl/bookie/chat" -Headers $headers -Body $jsonBody
    Start-Sleep -Milliseconds 300
    $sseResults += Invoke-SseChat -Url "$BaseUrl/bookie/chat/stream" -Headers $streamHeaders -Body $jsonBody
    Start-Sleep -Milliseconds 300
}

$jsonAvg = [math]::Round((($jsonResults | Measure-Object -Property ElapsedMs -Average).Average), 2)
$sseFirstAvg = [math]::Round((($sseResults | Measure-Object -Property FirstEventMs -Average).Average), 2)
$sseTotalAvg = [math]::Round((($sseResults | Measure-Object -Property TotalMs -Average).Average), 2)

Write-Host ""
Write-Host "JSON /bookie/chat"
$jsonResults | Format-Table -AutoSize
Write-Host "Average total: $jsonAvg ms"

Write-Host ""
Write-Host "SSE /bookie/chat/stream"
$sseResults | Format-Table -AutoSize
Write-Host "Average first event: $sseFirstAvg ms"
Write-Host "Average total: $sseTotalAvg ms"

Write-Host ""
Write-Host "Interpretation"
Write-Host "- /bookie/chat compares full response completion."
Write-Host "- /bookie/chat/stream first-event compares perceived responsiveness."
Write-Host "- /bookie/chat/stream total compares full stream completion."
