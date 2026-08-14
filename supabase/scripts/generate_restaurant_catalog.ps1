param(
    [string]$OutputPath = (Join-Path (Resolve-Path (Join-Path $PSScriptRoot '..\migrations')) '20260814183000_restaurant_catalog_osm_data.sql'),
    [int]$PerCity = 60,
    [int]$RadiusMeters = 3000,
    [string]$OverpassEndpoint = 'https://maps.mail.ru/osm/tools/overpass/api/interpreter',
    [string[]]$OnlyCityKeys = @()
)

$ErrorActionPreference = 'Stop'

$cities = @(
    [pscustomobject]@{ key = 'prague'; ru = 'Прага'; en = 'Prague'; es = 'Praga'; de = 'Prag'; lat = 50.0755; lon = 14.4378 },
    [pscustomobject]@{ key = 'dresden'; ru = 'Дрезден'; en = 'Dresden'; es = 'Dresde'; de = 'Dresden'; lat = 51.0504; lon = 13.7373 },
    [pscustomobject]@{ key = 'berlin'; ru = 'Берлин'; en = 'Berlin'; es = 'Berlín'; de = 'Berlin'; lat = 52.5200; lon = 13.4050 },
    [pscustomobject]@{ key = 'moscow'; ru = 'Москва'; en = 'Moscow'; es = 'Moscú'; de = 'Moskau'; lat = 55.7558; lon = 37.6173 },
    [pscustomobject]@{ key = 'salzburg'; ru = 'Зальцбург'; en = 'Salzburg'; es = 'Salzburgo'; de = 'Salzburg'; lat = 47.8095; lon = 13.0550 },
    [pscustomobject]@{ key = 'verona'; ru = 'Верона'; en = 'Verona'; es = 'Verona'; de = 'Verona'; lat = 45.4384; lon = 10.9916 },
    [pscustomobject]@{ key = 'rome'; ru = 'Рим'; en = 'Rome'; es = 'Roma'; de = 'Rom'; lat = 41.9028; lon = 12.4964 },
    [pscustomobject]@{ key = 'pisa'; ru = 'Пиза'; en = 'Pisa'; es = 'Pisa'; de = 'Pisa'; lat = 43.7228; lon = 10.4017 },
    [pscustomobject]@{ key = 'figline valdarno'; ru = 'Фильине-Вальдарно'; en = 'Figline Valdarno'; es = 'Figline Valdarno'; de = 'Figline Valdarno'; lat = 43.6190; lon = 11.4690 },
    [pscustomobject]@{ key = 'san marino'; ru = 'Сан-Марино'; en = 'San Marino'; es = 'San Marino'; de = 'San Marino'; lat = 43.9424; lon = 12.4578 },
    [pscustomobject]@{ key = 'chioggia'; ru = 'Кьоджа'; en = 'Chioggia'; es = 'Chioggia'; de = 'Chioggia'; lat = 45.2181; lon = 12.2786 },
    [pscustomobject]@{ key = 'milan'; ru = 'Милан'; en = 'Milan'; es = 'Milán'; de = 'Mailand'; lat = 45.4642; lon = 9.1900 },
    [pscustomobject]@{ key = 'valdidentro'; ru = 'Вальдидентро'; en = 'Valdidentro'; es = 'Valdidentro'; de = 'Valdidentro'; lat = 46.4890; lon = 10.2940 },
    [pscustomobject]@{ key = 'ravensburg'; ru = 'Равенсбург'; en = 'Ravensburg'; es = 'Ravensburg'; de = 'Ravensburg'; lat = 47.7810; lon = 9.6110 },
    [pscustomobject]@{ key = 'munich'; ru = 'Мюнхен'; en = 'Munich'; es = 'Múnich'; de = 'München'; lat = 48.1351; lon = 11.5820 },
    [pscustomobject]@{ key = 'vienna'; ru = 'Вена'; en = 'Vienna'; es = 'Viena'; de = 'Wien'; lat = 48.2082; lon = 16.3738 },
    [pscustomobject]@{ key = 'innsbruck'; ru = 'Инсбрук'; en = 'Innsbruck'; es = 'Innsbruck'; de = 'Innsbruck'; lat = 47.2692; lon = 11.4041 },
    [pscustomobject]@{ key = 'florence'; ru = 'Флоренция'; en = 'Florence'; es = 'Florencia'; de = 'Florenz'; lat = 43.7696; lon = 11.2558 },
    [pscustomobject]@{ key = 'venice'; ru = 'Венеция'; en = 'Venice'; es = 'Venecia'; de = 'Venedig'; lat = 45.4408; lon = 12.3155 },
    [pscustomobject]@{ key = 'tallinn'; ru = 'Таллин'; en = 'Tallinn'; es = 'Tallin'; de = 'Tallinn'; lat = 59.4370; lon = 24.7536 },
    [pscustomobject]@{ key = 'riga'; ru = 'Рига'; en = 'Riga'; es = 'Riga'; de = 'Riga'; lat = 56.9496; lon = 24.1052 },
    [pscustomobject]@{ key = 'vilnius'; ru = 'Вильнюс'; en = 'Vilnius'; es = 'Vilna'; de = 'Vilnius'; lat = 54.6872; lon = 25.2797 },
    [pscustomobject]@{ key = 'castel gandolfo'; ru = 'Кастель-Гандольфо'; en = 'Castel Gandolfo'; es = 'Castel Gandolfo'; de = 'Castel Gandolfo'; lat = 41.7475; lon = 12.6500 },
    [pscustomobject]@{ key = 'lake como'; ru = 'Озеро Комо'; en = 'Lake Como'; es = 'Lago di Como'; de = 'Comer See'; lat = 45.8080; lon = 9.2600 },
    [pscustomobject]@{ key = 'como'; ru = 'Комо'; en = 'Como'; es = 'Como'; de = 'Como'; lat = 45.8080; lon = 9.2600 },
    [pscustomobject]@{ key = 'bormio'; ru = 'Бормио'; en = 'Bormio'; es = 'Bormio'; de = 'Bormio'; lat = 46.4670; lon = 10.3740 },
    [pscustomobject]@{ key = 'val viola valley'; ru = 'Долина Валь-Виола'; en = 'Val Viola Valley'; es = 'Valle de Val Viola'; de = 'Val Viola-Tal'; lat = 46.4200; lon = 10.1900 },
    [pscustomobject]@{ key = 'stelvio'; ru = 'Стельвио'; en = 'Stelvio'; es = 'Stelvio'; de = 'Stilfser Joch'; lat = 46.5286; lon = 10.4540 }
)

if ($OnlyCityKeys.Count -gt 0) {
    $cities = @($cities | Where-Object { $_.key -in $OnlyCityKeys })
}

function Get-TagValue($tags, [string]$name) {
    if ($null -eq $tags) { return '' }
    $property = $tags.PSObject.Properties[$name]
    if ($null -eq $property -or $null -eq $property.Value) { return '' }
    return ([string]$property.Value).Trim()
}

function Sql-Text([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return "''" }
    $value = $value -replace '[\r\n\t]+', ' '
    return "'" + $value.Replace("'", "''") + "'"
}

function Sql-Number($value) {
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) { return 'null' }
    return ([double]$value).ToString('0.######', [Globalization.CultureInfo]::InvariantCulture)
}

function Get-OsmRestaurants($city) {
    # Node-only results keep the public Overpass request small and fast. They
    # cover the restaurant markers users normally see in city search; polygon
    # venues can be added later without changing the app schema.
    $query = '[out:json][timeout:30];node(around:' + $RadiusMeters + ',' + $city.lat + ',' + $city.lon + ')["amenity"="restaurant"]["name"];out tags 100;'
    $encodedQuery = [Uri]::EscapeDataString($query)
    $uri = "${OverpassEndpoint}?data=$encodedQuery"
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 120
            $rawBytes = $response.RawContentStream.ToArray()
            return (([Text.Encoding]::UTF8.GetString($rawBytes)) | ConvertFrom-Json).elements
        } catch {
            if ($attempt -eq 3) { throw }
            Start-Sleep -Seconds 2
        }
    }
}

$rows = New-Object System.Collections.Generic.List[object]
foreach ($city in $cities) {
    Write-Host "Loading $($city.en)..."
    try {
        $elements = @(Get-OsmRestaurants $city)
    } catch {
        Write-Warning "Skipping $($city.en) after Overpass retries: $($_.Exception.Message)"
        continue
    }
    $candidateRows = foreach ($element in $elements) {
        $tags = $element.tags
        $nativeName = Get-TagValue $tags 'name'
        if ([string]::IsNullOrWhiteSpace($nativeName)) { continue }

        $nameRu = Get-TagValue $tags 'name:ru'
        if ([string]::IsNullOrWhiteSpace($nameRu)) { $nameRu = $nativeName }
        $nameEn = Get-TagValue $tags 'name:en'
        if ([string]::IsNullOrWhiteSpace($nameEn)) { $nameEn = $nativeName }
        $nameEs = Get-TagValue $tags 'name:es'
        if ([string]::IsNullOrWhiteSpace($nameEs)) { $nameEs = $nameEn }
        $nameDe = Get-TagValue $tags 'name:de'
        if ([string]::IsNullOrWhiteSpace($nameDe)) { $nameDe = $nameEn }

        $street = Get-TagValue $tags 'addr:street'
        $house = Get-TagValue $tags 'addr:housenumber'
        $postcode = Get-TagValue $tags 'addr:postcode'
        $addrCity = Get-TagValue $tags 'addr:city'
        $address = (@($street, $house) | Where-Object { $_ }) -join ' '
        $address = (@($address, $postcode, $addrCity) | Where-Object { $_ }) -join ', '
        $cuisine = (Get-TagValue $tags 'cuisine').Replace(';', ', ')
        $website = Get-TagValue $tags 'contact:website'
        if ([string]::IsNullOrWhiteSpace($website)) { $website = Get-TagValue $tags 'website' }
        $phone = Get-TagValue $tags 'contact:phone'
        if ([string]::IsNullOrWhiteSpace($phone)) { $phone = Get-TagValue $tags 'phone' }
        $latitude = if ($null -ne $element.lat) { $element.lat } elseif ($null -ne $element.center) { $element.center.lat } else { $null }
        $longitude = if ($null -ne $element.lon) { $element.lon } elseif ($null -ne $element.center) { $element.center.lon } else { $null }
        $hasExtraData = if ($website -or $address -or $cuisine) { 0 } else { 1 }
        $id = "osm-restaurant-$($element.type)-$($element.id)"
        [pscustomobject]@{
            id = $id
            city = $city
            nameRu = $nameRu
            nameEn = $nameEn
            nameEs = $nameEs
            nameDe = $nameDe
            cuisine = $cuisine
            address = $address
            website = $website
            phone = $phone
            latitude = $latitude
            longitude = $longitude
            mapUrl = "https://www.openstreetmap.org/$($element.type)/$($element.id)"
            searchText = (($nameRu, $nameEn, $nameEs, $nameDe, $cuisine, $address) -join '|').ToLowerInvariant()
            rank = $hasExtraData
        }
    }

    $uniqueRows = $candidateRows |
        Group-Object { "$($_.nameEn.ToLowerInvariant())|$($_.address.ToLowerInvariant())" } |
        ForEach-Object { $_.Group | Select-Object -First 1 } |
        Sort-Object rank, nameEn |
        Select-Object -First $PerCity
    $sortOrder = 0
    foreach ($row in $uniqueRows) {
        $row | Add-Member -NotePropertyName sortOrder -NotePropertyValue $sortOrder
        $rows.Add($row)
        $sortOrder++
    }
    Write-Host "  selected $($uniqueRows.Count) of $($elements.Count)"
    Start-Sleep -Milliseconds 400
}

# Lake Como and Como intentionally share an area and can return the same OSM
# node. Keep one canonical row per OSM id so every insert batch is conflict-safe.
$rows = @($rows | Group-Object id | ForEach-Object { $_.Group | Select-Object -First 1 })

$columns = 'id, city_key, city_name_ru, city_name_en, city_name_es, city_name_de, name_ru, name_en, name_es, name_de, cuisine, address, website, phone, latitude, longitude, map_url, search_text, sort_order'
$values = foreach ($row in $rows) {
    $city = $row.city
    '(' + (@(
        (Sql-Text $row.id),
        (Sql-Text $city.key),
        (Sql-Text $city.ru),
        (Sql-Text $city.en),
        (Sql-Text $city.es),
        (Sql-Text $city.de),
        (Sql-Text $row.nameRu),
        (Sql-Text $row.nameEn),
        (Sql-Text $row.nameEs),
        (Sql-Text $row.nameDe),
        (Sql-Text $row.cuisine),
        (Sql-Text $row.address),
        (Sql-Text $row.website),
        (Sql-Text $row.phone),
        (Sql-Number $row.latitude),
        (Sql-Number $row.longitude),
        (Sql-Text $row.mapUrl),
        (Sql-Text $row.searchText),
        $row.sortOrder
    ) -join ', ') + ')'
}

$sql = @"
-- OSM restaurant catalog generated on $(Get-Date -Format 'yyyy-MM-dd').
-- Data © OpenStreetMap contributors, licensed under the ODbL.
insert into public.restaurant_catalog ($columns) values
$($values -join ",`n")
on conflict (id) do update set
    city_key = excluded.city_key,
    city_name_ru = excluded.city_name_ru,
    city_name_en = excluded.city_name_en,
    city_name_es = excluded.city_name_es,
    city_name_de = excluded.city_name_de,
    name_ru = excluded.name_ru,
    name_en = excluded.name_en,
    name_es = excluded.name_es,
    name_de = excluded.name_de,
    cuisine = excluded.cuisine,
    address = excluded.address,
    website = excluded.website,
    phone = excluded.phone,
    latitude = excluded.latitude,
    longitude = excluded.longitude,
    map_url = excluded.map_url,
    search_text = excluded.search_text,
    sort_order = excluded.sort_order;
"@

$parent = Split-Path -Parent $OutputPath
if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath), $sql, (New-Object Text.UTF8Encoding($false)))
Write-Host "Wrote $($rows.Count) rows to $OutputPath"
