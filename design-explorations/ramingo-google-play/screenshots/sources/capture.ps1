param([ValidateSet('capture','tap','back','swipe','section')][string]$Action='capture',[int]$X,[int]$Y,[int]$ToX,[int]$ToY,[string]$Name='current',[string]$Section,[string]$DeviceSerial)
$ErrorActionPreference='Stop'
$listingAdb=(Get-Command adb).Source
$listingDevice=$DeviceSerial
if(-not $listingDevice) {
 $listingReady=@(& $listingAdb devices | Select-String '^\S+\s+device$')
 if($listingReady.Count -ne 1){throw 'Specify -DeviceSerial when there is not exactly one authorized device.'}
 $listingDevice=($listingReady[0].Line -split '\s+')[0]
}
$listingWork=Join-Path (Get-Location) 'tmp/ramingo-listing-capture'
New-Item -ItemType Directory -Force $listingWork | Out-Null
function Read-ListingTree([string]$Target) {
 for($listingTry=0;$listingTry -lt 3;$listingTry++) {
  $listingDump=(& $listingAdb -s $listingDevice shell uiautomator dump /sdcard/ramingo-listing.xml 2>&1 | Out-String)
  if($listingDump -match 'dumped to:') { & $listingAdb -s $listingDevice pull /sdcard/ramingo-listing.xml $Target 2>$null | Out-Null; return }
 }
 throw 'No fresh UI tree; no further action was taken.'
}
if($Action -ne 'capture') {
 $listingFocus=(& $listingAdb -s $listingDevice shell dumpsys activity activities | Select-String 'topResumedActivity=' | Out-String)
 if($listingFocus -notmatch 'com\.odyssey\.travelplanner') {throw 'Ramingo is not the foreground app; no input sent.'}
}
switch($Action) {
 'tap' { & $listingAdb -s $listingDevice shell input tap $X $Y }
 'swipe' { & $listingAdb -s $listingDevice shell input swipe $X $Y $ToX $ToY 450 }
 'back' { & $listingAdb -s $listingDevice shell input keyevent 4 }
 'section' {
  & $listingAdb -s $listingDevice shell input tap 60 118
  Read-ListingTree "$listingWork/menu.xml"
  [xml]$listingMenu=Get-Content "$listingWork/menu.xml"
  $listingItems=@($listingMenu.SelectNodes('//node') | Where-Object {$_.text -eq $Section})
  if($listingItems.Count -ne 1){throw 'Section was not uniquely visible; stopped.'}
  if($listingItems[0].bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$'){throw 'Invalid bounds'}
  & $listingAdb -s $listingDevice shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
 }
}
Read-ListingTree "$listingWork/$Name.xml"
& $listingAdb -s $listingDevice shell screencap -p /sdcard/ramingo-listing.png
& $listingAdb -s $listingDevice pull /sdcard/ramingo-listing.png "$listingWork/$Name.png" 2>$null | Out-Null
[xml]$listingTree=Get-Content "$listingWork/$Name.xml"
$listingTree.SelectNodes('//node') | Where-Object {$_.password -ne 'true' -and ($_.text -or $_.'content-desc')} | ForEach-Object {[pscustomobject]@{text=$_.text;description=$_.'content-desc';bounds=$_.bounds}} | ConvertTo-Json -Compress
