package com.odyssey.travelplanner.ui.i18n

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.setValue
import com.odyssey.travelplanner.data.normalizeCatalogText
import com.odyssey.travelplanner.data.isPlaceholderSightDescription
import java.util.Locale

@Composable
internal fun localizedSightName(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "арена ди верона" -> localized("Арена ди Верона", "Verona Arena", "Arena de Verona", "Arena von Verona")
    "пьяцца бра" -> localized("Пьяцца Бра", "Piazza Bra", "Piazza Bra", "Piazza Bra")
    "дом джульетты" -> localized("Дом Джульетты", "Juliet's House", "Casa de Julieta", "Julias Haus")
    "пьяцца делле эрбе" -> localized("Пьяцца делле Эрбе", "Piazza delle Erbe", "Piazza delle Erbe", "Piazza delle Erbe")
    "большой цирк" -> localized("Большой цирк", "Circus Maximus", "Circo Máximo", "Circus Maximus")
    "термы каракаллы" -> localized("Термы Каракаллы", "Baths of Caracalla", "Termas de Caracalla", "Caracalla-Thermen")
    "уста истины" -> localized("Уста Истины", "Mouth of Truth", "Boca de la Verdad", "Mund der Wahrheit")
    "район монти", "district монти" -> localized("Район Монти", "Monti district", "Barrio de Monti", "Viertel Monti")
    "кастель-гандольфо" -> localized("Кастель-Гандольфо", "Castel Gandolfo", "Castel Gandolfo", "Castel Gandolfo")
    "озеро альбано" -> localized("Озеро Альбано", "Lake Albano", "Lago Albano", "Lago Albano")
    "антико спедале серристори" -> localized("Антико Спедале Серристори", "Antico Spedale Serristori", "Antico Spedale Serristori", "Antico Spedale Serristori")
    "пьяцца марсилио фичино" -> localized("Пьяцца Марсилио Фичино", "Piazza Marsilio Ficino", "Piazza Marsilio Ficino", "Piazza Marsilio Ficino")
    "палаццо преторио" -> localized("Палаццо Преторио", "Palazzo Pretorio", "Palazzo Pretorio", "Palazzo Pretorio")
    "коллегиата санта-мария" -> localized("Коллегиата Санта-Мария", "Collegiate Church of Santa Maria", "Colegiata de Santa Maria", "Stiftskirche Santa Maria")
    "корсо-дель-пополо" -> localized("Корсо-дель-Пополо", "Corso del Popolo", "Corso del Popolo", "Corso del Popolo")
    "собор санта-мария-ассунта", "кафедральный собор santa maria assunta" -> localized("Собор Санта-Мария-Ассунта", "Santa Maria Assunta Cathedral", "Catedral de Santa Maria Assunta", "Kathedrale Santa Maria Assunta")
    "канал вена" -> localized("Канал Вена", "Vena Canal", "Canal Vena", "Vena-Kanal")
    "палаццо гранайо" -> localized("Палаццо Гранайо", "Palazzo Granaio", "Palazzo Granaio", "Palazzo Granaio")
    "дуомо (миланский собор)", "дуомо (миланский cathedral)" -> localized("Дуомо (Миланский собор)", "Milan Cathedral (Duomo)", "Catedral de Milán (Duomo)", "Mailänder Dom (Duomo)")
    "галерея виктора эммануила ii" -> localized("Галерея Виктора Эммануила II", "Galleria Vittorio Emanuele II", "Galería Vittorio Emanuele II", "Galleria Vittorio Emanuele II")
    "театр ла скала", "theatre ла скала" -> localized("Театр Ла Скала", "La Scala Theatre", "Teatro alla Scala", "Teatro alla Scala")
    "пинакотека брера" -> localized("Пинакотека Брера", "Brera Gallery", "Pinacoteca de Brera", "Pinacoteca di Brera")
    "монументальное кладбище" -> localized("Монументальное кладбище", "Monumental Cemetery", "Cementerio Monumental", "Monumentalfriedhof")
    "площадь гае ауленти", "square гае ауленти" -> localized("Площадь Гае Ауленти", "Piazza Gae Aulenti", "Piazza Gae Aulenti", "Piazza Gae Aulenti")
    "боско вертикале" -> localized("Боско Вертикале", "Bosco Verticale", "Bosco Verticale", "Bosco Verticale")
    "центральный вокзал милана" -> localized("Центральный вокзал Милана", "Milan Central Station", "Estación Central de Milán", "Mailänder Hauptbahnhof")
    "комо (город)" -> localized("Комо (город)", "Como (city)", "Como (ciudad)", "Como (Stadt)")
    "черноббио (вилла д’эсте)" -> localized("Черноббио (Вилла д’Эсте)", "Cernobbio (Villa d’Este)", "Cernobbio (Villa d’Este)", "Cernobbio (Villa d’Este)")
    "вилла бальбьянелло (ленно)" -> localized("Вилла Бальбьянелло (Ленно)", "Villa del Balbianello (Lenno)", "Villa del Balbianello (Lenno)", "Villa del Balbianello (Lenno)")
    "вилла карлотта (тремеццо)" -> localized("Вилла Карлотта (Тремеццо)", "Villa Carlotta (Tremezzo)", "Villa Carlotta (Tremezzo)", "Villa Carlotta (Tremezzo)")
    "арнога" -> localized("Арнога", "Arnoga", "Arnoga", "Arnoga")
    "долина валь-виола" -> localized("Долина Валь-Виола", "Val Viola Valley", "Valle de Val Viola", "Val Viola-Tal")
    "башни фраэле" -> localized("Башни Фраэле", "Fraele Towers", "Torres Fraele", "Fraele-Türme")
    "озеро делле-скале" -> localized("Озеро делле-Скале", "Lake delle Scale", "Lago delle Scale", "Lago delle Scale")
    "арнога — старт стельвио" -> localized("Арнога — старт Стельвио", "Arnoga — Stelvio start", "Arnoga — inicio del Stelvio", "Arnoga — Stelvio-Start")
    "бормио — старый город", "бормио — old town" -> localized("Бормио — старый город", "Bormio — Old Town", "Bormio — casco antiguo", "Bormio — Altstadt")
    "баньи-веки — панорама бормио" -> localized("Баньи-Векки — панорама Бормио", "Bagni Vecchi — Bormio panorama", "Bagni Vecchi — panorama de Bormio", "Bagni Vecchi — Panorama von Bormio")
    "перевал стельвио" -> localized("Перевал Стельвио", "Stelvio Pass", "Paso del Stelvio", "Stilfser Joch")
    "мариенплац и новая ратуша", "мариенплац и new town hall" -> localized("Мариенплац и Новая ратуша", "Marienplatz and New Town Hall", "Marienplatz y el Ayuntamiento Nuevo", "Marienplatz und Neues Rathaus")
    "виктуалиенмаркт" -> localized("Виктуалиенмаркт", "Viktualienmarkt", "Viktualienmarkt", "Viktualienmarkt")
    "одеонсплац" -> localized("Одеонсплац", "Odeonsplatz", "Odeonsplatz", "Odeonsplatz")
    "хофгартен" -> localized("Хофгартен", "Hofgarten", "Hofgarten", "Hofgarten")
    "староместская площадь и часы орлой", "староместская square и часы орлой" -> localized("Староместская площадь и часы Орлой", "Old Town Square and Orloj", "Plaza de la Ciudad Vieja y el Orloj", "Altstädter Ring und Orloj")
    "клементинум" -> localized("Клементинум", "Klementinum", "Klementinum", "Klementinum")
    "карлов мост", "карлов bridge" -> localized("Карлов мост", "Charles Bridge", "Puente de Carlos", "Karlsbrücke")
    "малостранская площадь", "малостранская square" -> localized("Малостранская площадь", "Malá Strana Square", "Plaza de Malá Strana", "Kleinseitner Ring")
    "арена вероны (arena di verona)" -> localized("Арена Вероны (Arena di Verona)", "Verona Arena (Arena di Verona)", "Arena de Verona (Arena di Verona)", "Arena von Verona (Arena di Verona)")
    "рождественская звезда rigoletto" -> localized("Рождественская звезда Rigoletto", "Rigoletto Christmas star", "Estrella navideña Rigoletto", "Weihnachtsstern Rigoletto")
    "набережная реки адидже" -> localized("Набережная реки Адидже", "Adige riverfront", "Paseo del río Adigio", "Uferpromenade am Etsch")
    "фонтан четырех рек" -> localized("Фонтан Четырёх рек", "Fountain of the Four Rivers", "Fuente de los Cuatro Ríos", "Brunnen der Vier Flüsse")
    "церковь sant'agnese in agone" -> localized("Церковь Sant'Agnese in Agone", "Sant'Agnese in Agone Church", "Iglesia de Sant'Agnese in Agone", "Kirche Sant'Agnese in Agone")
    "храм адриана" -> localized("Храм Адриана", "Temple of Hadrian", "Templo de Adriano", "Tempel des Hadrian")
    "колонна марка аврелия" -> localized("Колонна Марка Аврелия", "Column of Marcus Aurelius", "Columna de Marco Aurelio", "Säule des Marc Aurel")
    "фонтан треви" -> localized("Фонтан Треви", "Trevi Fountain", "Fontana di Trevi", "Trevi-Brunnen")
    "испанская лестница" -> localized("Испанская лестница", "Spanish Steps", "Escalinata de España", "Spanische Treppe")
    "рождественская ёлка на piazza di spagna" -> localized("Рождественская ёлка на Piazza di Spagna", "Christmas tree at Piazza di Spagna", "Árbol de Navidad en Piazza di Spagna", "Weihnachtsbaum an der Piazza di Spagna")
    "колизей" -> localized("Колизей", "Colosseum", "Coliseo", "Kolosseum")
    "арка константина" -> localized("Арка Константина", "Arch of Constantine", "Arco de Constantino", "Konstantinsbogen")
    "римский форум" -> localized("Римский форум", "Roman Forum", "Forum Romanum", "Forum Romanum")
    "палатинский холм" -> localized("Палатинский холм", "Palatine Hill", "Monte Palatino", "Palatin")
    "смотровая площадка на форум" -> localized("Смотровая площадка на Форум", "Forum viewpoint", "Mirador del Foro", "Aussichtspunkt auf das Forum")
    "капитолийская площадь" -> localized("Капитолийская площадь", "Capitoline Square", "Plaza del Campidoglio", "Kapitolsplatz")
    "площадь святого петра" -> localized("Площадь Святого Петра", "St. Peter's Square", "Plaza de San Pedro", "Petersplatz")
    "собор святого петра" -> localized("Собор Святого Петра", "St. Peter's Basilica", "Basílica de San Pedro", "Petersdom")
    "главная рождественская ёлка ватикана" -> localized("Главная рождественская ёлка Ватикана", "Vatican's main Christmas tree", "Árbol de Navidad principal del Vaticano", "Hauptweihnachtsbaum des Vatikans")
    "рождественский вертеп" -> localized("Рождественский вертеп", "Christmas nativity scene", "Belén navideño", "Weihnachtskrippe")
    "мост скальци" -> localized("Мост Скальци", "Scalzi Bridge", "Puente de los Descalzos", "Scalzi-Brücke")
    "прогулка вдоль гранд-канала" -> localized("Прогулка вдоль Гранд-канала", "Grand Canal walk", "Paseo por el Gran Canal", "Spaziergang am Canal Grande")
    "вапоретто по гранд-каналу" -> localized("Вапоретто по Гранд-каналу", "Vaporetto along the Grand Canal", "Vaporetto por el Gran Canal", "Vaporetto auf dem Canal Grande")
    "мост риальто" -> localized("Мост Риальто", "Rialto Bridge", "Puente de Rialto", "Rialtobrücke")
    "рынок риальто" -> localized("Рынок Риальто", "Rialto Market", "Mercado de Rialto", "Rialto-Markt")
    "улочки района сан-поло" -> localized("Улочки района Сан-Поло", "San Polo's lanes", "Calles del barrio de San Polo", "Gassen im Viertel San Polo")
    "базилика санта-мария-глориоза-деи-фрари" -> localized("Базилика Санта-Мария-Глориоза-деи-Фрари", "Basilica of Santa Maria Gloriosa dei Frari", "Basílica de Santa Maria Gloriosa dei Frari", "Basilika Santa Maria Gloriosa dei Frari")
    "мост вздохов" -> localized("Мост Вздохов", "Bridge of Sighs", "Puente de los Suspiros", "Seufzerbrücke")
    "площадь сан-марко" -> localized("Площадь Сан-Марко", "St. Mark's Square", "Plaza de San Marcos", "Markusplatz")
    "собор святого марка" -> localized("Собор Святого Марка", "St. Mark's Basilica", "Basílica de San Marcos", "Markusdom")
    "колонна святого марка" -> localized("Колонна Святого Марка", "Column of St. Mark", "Columna de San Marcos", "Säule des heiligen Markus")
    "канал vena" -> localized("Канал Vena", "Vena Canal", "Canal Vena", "Vena-Kanal")
    "кафедральный собор santa maria assunta" -> localized("Кафедральный собор Santa Maria Assunta", "Santa Maria Assunta Cathedral", "Catedral de Santa Maria Assunta", "Kathedrale Santa Maria Assunta")
    "церковь sant'andrea" -> localized("Церковь Sant'Andrea", "Sant'Andrea Church", "Iglesia de Sant'Andrea", "Kirche Sant'Andrea")
    "мостики через канал vena" -> localized("Мостики через канал Vena", "Bridges over Vena Canal", "Puentes sobre el canal Vena", "Brücken über den Vena-Kanal")
    "рыбацкие домики и пришвартованные лодки" -> localized("Рыбацкие домики и пришвартованные лодки", "Fishing cottages and moored boats", "Casas de pescadores y barcos amarrados", "Fischerhäuser und vertäute Boote")
    "набережная лагуны" -> localized("Набережная лагуны", "Lagoon waterfront", "Paseo de la laguna", "Lagunenpromenade")
    "порт кьоджи" -> localized("Порт Кьоджи", "Chioggia port", "Puerto de Chioggia", "Hafen von Chioggia")
    "прогулка по дамбе diga sottomarina" -> localized("Прогулка по дамбе Diga Sottomarina", "Diga Sottomarina dike walk", "Paseo por el dique Diga Sottomarina", "Spaziergang auf dem Damm Diga Sottomarina")
    "панорамные виды на лагуну" -> localized("Панорамные виды на лагуну", "Panoramic lagoon views", "Vistas panorámicas de la laguna", "Panoramablick auf die Lagune")
    "главная рождественская ёлка города" -> localized("Главная рождественская ёлка города", "City's main Christmas tree", "Árbol de Navidad principal de la ciudad", "Hauptweihnachtsbaum der Stadt")
    "рождественские ярмарочные домики" -> localized("Рождественские ярмарочные домики", "Christmas market stalls", "Casetas del mercado navideño", "Weihnachtsmarktbuden")
    else -> localizedSightNameByTerms(value)
}

@Composable
internal fun localizedSightDescription(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    val known = when {
        normalized.contains("античная мраморная маска") -> localized("Античная мраморная маска в портике церкви Санта-Мария-ин-Космедин — по легенде откусит руку лжецу.", "Ancient marble mask at the portico of Santa Maria in Cosmedin; legend says it bites the hand of a liar.", "Máscara de mármol antigua en el pórtico de Santa Maria in Cosmedin; según la leyenda, muerde la mano del mentiroso.", "Antike Marmormaske im Portikus von Santa Maria in Cosmedin; der Legende nach beißt sie die Hand eines Lügners.")
        normalized.contains("атмосферный старинный район") -> localized("Атмосферный старинный район у Форума: ремесленные лавки, винные бары и вечерняя жизнь.", "Atmospheric historic district by the Forum, with artisan shops, wine bars, and lively evenings.", "Barrio histórico con ambiente junto al Foro, tiendas de artesanía, bares de vino y vida nocturna.", "Stimmungsvolles historisches Viertel am Forum mit Handwerksläden, Weinbars und regem Abendleben.")
        normalized.contains("однодневная поездка из рима") -> localized("Однодневная поездка из Рима: исторический центр и Апостольский дворец.", "A day trip from Rome: historic center and Apostolic Palace.", "Excursión de un día desde Roma: centro histórico y Palacio Apostólico.", "Tagesausflug von Rom: historisches Zentrum und Apostolischer Palast.")
        normalized.contains("вулканическое озеро") -> localized("Вулканическое озеро рядом с Кастель-Гандольфо.", "Volcanic lake near Castel Gandolfo.", "Lago volcánico cerca de Castel Gandolfo.", "Vulkanischer See bei Castel Gandolfo.")
        normalized.contains("средневековый госпиталь") -> localized("Средневековый госпиталь, основанный семьёй Серристори в XIV веке. Сохранил свою церковь и алтарь XV века; сегодня — культурный центр и музей аптечной посуды.", "Medieval hospital founded by the Serristori family in the 14th century. It retains its church and 15th-century altar and is now a cultural center and museum of apothecary ceramics.", "Hospital medieval fundado por la familia Serristori en el siglo XIV. Conserva su iglesia y un altar del siglo XV; hoy es un centro cultural y museo de cerámica farmacéutica.", "Mittelalterliches Hospital, im 14. Jahrhundert von der Familie Serristori gegründet. Mit eigener Kirche und Altar aus dem 15. Jahrhundert ist es heute Kulturzentrum und Museum für Apothekenkeramik.")
        normalized.contains("сердце старого города") -> localized("Сердце Старого города — одна из самых больших средневековых площадей Тосканы, окружённая портиками. По воскресеньям здесь антикварный рынок. Названа в честь философа Марсилио Фичино, родившегося в Фильине в 1433 году.", "The heart of the Old Town: one of Tuscany's largest medieval squares, lined with arcades. An antiques market is held here on Sundays. It is named after philosopher Marsilio Ficino, born in Figline in 1433.", "El corazón del casco antiguo: una de las plazas medievales más grandes de la Toscana, rodeada de soportales. Los domingos acoge un mercado de antigüedades. Lleva el nombre del filósofo Marsilio Ficino, nacido en Figline en 1433.", "Das Herz der Altstadt: einer der größten mittelalterlichen Plätze der Toskana, von Arkaden gesäumt. Sonntags findet hier ein Antiquitätenmarkt statt. Benannt ist er nach dem Philosophen Marsilio Ficino, der 1433 in Figline geboren wurde.")
        normalized.contains("историческая резиденция подеста") -> localized("Историческая резиденция подеста в центре города; фасад украшен гербами прежних правителей.", "Historic residence of the podestà in the city center; its façade is decorated with the coats of arms of former rulers.", "Residencia histórica del podestà en el centro; su fachada está decorada con los escudos de antiguos gobernantes.", "Historische Residenz des Podestà im Stadtzentrum; die Fassade ist mit den Wappen früherer Herrscher geschmückt.")
        normalized.contains("главная церковь города") -> localized("Главная церковь города. Хранит алтарный образ «Мадонна с Младенцем на троне» кисти Мастера из Фильине (после 1317 года).", "The city's main church. It houses the altarpiece Madonna and Child Enthroned by the Master of Figline, painted after 1317.", "La iglesia principal de la ciudad. Conserva el retablo Madonna con el Niño entronizada, obra del Maestro de Figline, posterior a 1317.", "Die Hauptkirche der Stadt. Sie beherbergt das Altarbild Madonna mit Kind auf dem Thron des Meisters von Figline aus der Zeit nach 1317.")
        normalized.contains("парадная главная улица") -> localized("Парадная главная улица-«салотто» Кьоджи, вытянутая через весь остров: дворцы, кафе и вечернее гулянье горожан.", "Chioggia's grand main boulevard, a salon-like street stretching across the island with palaces, cafés, and evening strolls.", "La gran calle principal de Chioggia, un paseo tipo salón que recorre la isla entre palacios, cafés y paseos al atardecer.", "Chioggias prächtige Hauptstraße, eine salonartige Flaniermeile über die Insel mit Palästen, Cafés und abendlichen Spaziergängen.")
        normalized.contains("кафедральный собор xvii века") -> localized("Кафедральный собор XVII века, перестроенный Бальдассаре Лонгеной, с отдельно стоящей колокольней XIV века.", "A 17th-century cathedral rebuilt by Baldassare Longhena, with a freestanding 14th-century bell tower.", "Catedral del siglo XVII reconstruida por Baldassare Longhena, con un campanario independiente del siglo XIV.", "Kathedrale aus dem 17. Jahrhundert, von Baldassare Longhena umgebaut, mit freistehendem Glockenturm aus dem 14. Jahrhundert.")
        normalized.contains("живописный главный канал") -> localized("Живописный главный канал с рыбацкими лодками и старыми мостами — за это Кьоджу зовут «маленькой Венецией».", "Scenic main canal with fishing boats and old bridges — why Chioggia is called Little Venice.", "Canal principal pintoresco con barcos pesqueros y puentes antiguos; por eso Chioggia recibe el nombre de Pequeña Venecia.", "Malerischer Hauptkanal mit Fischerbooten und alten Brücken — deshalb wird Chioggia Klein-Venedig genannt.")
        normalized.contains("городская житница") -> localized("Городская житница 1322 года на канале Вена; сегодня внизу — рыбный рынок и туристический офис.", "The city's 1322 granary on Vena Canal; today its ground floor houses a fish market and tourist office.", "Granero municipal de 1322 junto al canal Vena; hoy alberga un mercado de pescado y una oficina de turismo.", "Städtischer Getreidespeicher von 1322 am Vena-Kanal; heute befinden sich im Erdgeschoss ein Fischmarkt und ein Touristenbüro.")
        normalized.contains("готический собор из белого мрамора") -> localized("Готический собор из белого мрамора — символ Милана; можно подняться на крышу к шпилям и «Мадоннине».", "Gothic cathedral of white marble and symbol of Milan; climb to the rooftop spires and the Madonnina.", "Catedral gótica de mármol blanco y símbolo de Milán; se puede subir a la azotea, a las agujas y a la Madonnina.", "Gotischer Dom aus weißem Marmor und Wahrzeichen Mailands; auf dem Dach gelangt man zu den Türmen und der Madonnina.")
        normalized.contains("роскошная стеклянная галерея") -> localized("Роскошная стеклянная галерея XIX века рядом с собором — «гостиная Милана» с кафе и бутиками.", "A lavish 19th-century glass arcade beside the cathedral, known as Milan's salon, with cafés and boutiques.", "Galería acristalada del siglo XIX junto a la catedral, el salón de Milán, con cafés y boutiques.", "Prunkvolle Glasgalerie aus dem 19. Jahrhundert neben dem Dom, Mailands Salon mit Cafés und Boutiquen.")
        normalized.contains("легендарный оперный театр") -> localized("Легендарный оперный театр Ла Скала; при нём — музей театра.", "Legendary La Scala opera house with its own theater museum.", "Legendario teatro de ópera La Scala, con su propio museo teatral.", "Das legendäre Opernhaus La Scala mit eigenem Theatermuseum.")
        normalized.contains("одна из лучших картинных галерей италии") -> localized("Одна из лучших картинных галерей Италии в квартале Брера (Рафаэль, Караваджо, Мантенья).", "One of Italy's finest art galleries in the Brera district, with works by Raphael, Caravaggio, and Mantegna.", "Una de las mejores galerías de arte de Italia en el barrio de Brera, con obras de Rafael, Caravaggio y Mantegna.", "Eine der besten Kunstgalerien Italiens im Viertel Brera mit Werken von Raffael, Caravaggio und Mantegna.")
        normalized.contains("музей скульптуры под открытым небом") -> localized("Музей скульптуры под открытым небом: фамильные усыпальницы, модерн и надгробия-шедевры.", "Open-air sculpture museum with family tombs, Art Nouveau works, and masterpiece monuments.", "Museo de escultura al aire libre con mausoleos familiares, obras modernistas y monumentos funerarios.", "Skulpturenmuseum unter freiem Himmel mit Familiengrabmälern, Jugendstil und meisterhaften Grabdenkmälern.")
        normalized.contains("современная площадь порта-нуова") -> localized("Современная площадь Порта-Нуова с небоскрёбами, фонтанами и панорамой делового Милана.", "Modern Piazza Gae Aulenti in Porta Nuova, with skyscrapers, fountains, and views over Milan's business district.", "Moderna Piazza Gae Aulenti en Porta Nuova, con rascacielos, fuentes y vistas del distrito financiero de Milán.", "Moderner Platz Gae Aulenti in Porta Nuova mit Wolkenkratzern, Brunnen und Blick auf Mailands Geschäftsviertel.")
        normalized.contains("вертикальный лес") -> localized("«Вертикальный лес» — башни-небоскрёбы с деревьями на балконах в квартале Порта-Нуова.", "The Vertical Forest: skyscraper towers covered with trees on their balconies in Porta Nuova.", "El Bosque Vertical: torres de rascacielos con árboles en los balcones de Porta Nuova.", "Der Vertikale Wald: Wolkenkratzer mit Bäumen auf den Balkonen im Viertel Porta Nuova.")
        normalized.contains("монументальный вокзал") -> localized("Монументальный вокзал Milano Centrale с парадным фасадом, огромными сводчатыми залами и архитектурой в духе ар-деко.", "Monumental Milano Centrale station with a grand façade, vast vaulted halls, and Art Deco architecture.", "Monumental estación Milano Centrale, con una fachada grandiosa, enormes salas abovedadas y arquitectura art déco.", "Der monumentale Bahnhof Milano Centrale mit prächtiger Fassade, riesigen Gewölbehallen und Art-déco-Architektur.")
        normalized.contains("элегантный город у южного берега") -> localized("Элегантный город у южного берега: романский собор, набережная Пьяцца-Кавур и фуникулёр в Брунате с видом на озеро.", "Elegant city on the southern shore: a Romanesque cathedral, Piazza Cavour waterfront, and the funicular to Brunate overlooking the lake.", "Elegante ciudad en la orilla sur: catedral románica, paseo marítimo de Piazza Cavour y funicular a Brunate con vistas al lago.", "Elegante Stadt am Südufer mit romanischem Dom, Uferpromenade an der Piazza Cavour und Standseilbahn nach Brunate mit Seeblick.")
        normalized.contains("первый городок к северу") -> localized("Первый городок к северу от Комо; знаменита Вилла д’Эсте и живописная набережная.", "The first town north of Como, known for Villa d’Este and its scenic waterfront.", "El primer pueblo al norte de Como, famoso por Villa d’Este y su pintoresco paseo marítimo.", "Der erste Ort nördlich von Como, bekannt für die Villa d’Este und seine malerische Uferpromenade.")
        normalized.contains("романтическая вилла на мысу") -> localized("Романтическая вилла на мысу с террасными садами (снималась в «Звёздных войнах» и «Казино Рояль»); от Ленно — пешком или катером.", "Romantic villa on a promontory with terraced gardens, featured in Star Wars and Casino Royale; reachable from Lenno on foot or by boat.", "Villa romántica en un promontorio con jardines en terrazas, escenario de Star Wars y Casino Royale; desde Lenno se llega a pie o en barco.", "Romantische Villa auf einer Landzunge mit Terrassengärten, Drehort von Star Wars und Casino Royale; von Lenno zu Fuß oder per Boot erreichbar.")
        normalized.contains("вилла-музей со знаменитым ботаническим") -> localized("Вилла-музей со знаменитым ботаническим садом (азалии, рододендроны) и скульптурами.", "Villa museum with a famous botanical garden of azaleas and rhododendrons, plus sculptures.", "Museo-villa con un famoso jardín botánico de azaleas y rododendros, además de esculturas.", "Villa-Museum mit berühmtem botanischem Garten aus Azaleen und Rhododendren sowie Skulpturen.")
        normalized.contains("старт горной дороги") -> localized("Старт горной дороги в Валь-Виолу. Перед выездом проверьте погоду и статус высокогорных дорог.", "Start of the mountain road into Val Viola. Check the weather and high-mountain road status before departure.", "Inicio de la carretera de montaña hacia Val Viola. Compruebe el tiempo y el estado de las carreteras de alta montaña antes de salir.", "Beginn der Bergstraße ins Val Viola. Prüfen Sie vor der Abfahrt Wetter und Zustand der Hochgebirgsstraßen.")
        normalized.contains("широкая альпийская долина") -> localized("Широкая альпийская долина с лёгкими прогулками по грунтовой дороге и видами на вершины.", "Wide Alpine valley with easy walks along a dirt road and views of the peaks.", "Amplio valle alpino con paseos sencillos por un camino de tierra y vistas a las cumbres.", "Weites Alpental mit leichten Spaziergängen auf einer Schotterstraße und Blick auf die Gipfel.")
        normalized.contains("две средневековые башни") -> localized("Две средневековые башни над дорогой к озёрам Канкано; короткая остановка с панорамой долины.", "Two medieval towers above the road to the Cancano lakes; a short stop with a panoramic valley view.", "Dos torres medievales sobre la carretera a los lagos de Cancano; breve parada con vistas panorámicas del valle.", "Zwei mittelalterliche Türme über der Straße zu den Cancano-Seen; kurzer Halt mit Panoramablick ins Tal.")
        normalized.contains("высокогорное водохранилище") -> localized("Высокогорное водохранилище у Канкано, окружённое светлыми склонами и тропами.", "High-mountain reservoir near Cancano, surrounded by pale slopes and trails.", "Embalse de alta montaña cerca de Cancano, rodeado de laderas claras y senderos.", "Hochgebirgsstausee bei Cancano, umgeben von hellen Hängen und Wanderwegen.")
        normalized.contains("s.s. 301") -> localized("Старт: S.S. 301, Località Arnoga. В октябре утром обязательно проверьте открытие перевалов Стельвио и Умбраиль: снегопад может закрыть дорогу.", "Start: S.S. 301, Località Arnoga. In October, check early in the morning that the Stelvio and Umbrail passes are open: snowfall can close the road.", "Inicio: S.S. 301, Località Arnoga. En octubre, compruebe por la mañana que los puertos de Stelvio y Umbrail estén abiertos: la nieve puede cerrar la carretera.", "Start: S.S. 301, Località Arnoga. Prüfen Sie im Oktober morgens unbedingt, ob die Pässe Stilfser Joch und Umbrail geöffnet sind: Schneefall kann die Straße sperren.")
        normalized.contains("короткая остановка на кофе") -> localized("Короткая остановка на кофе и прогулку по старому центру перед подъёмом к перевалу.", "A short coffee stop and walk through the Old Town before climbing to the pass.", "Breve parada para tomar café y pasear por el casco antiguo antes de subir al puerto.", "Kurzer Kaffeestopp und Spaziergang durch die Altstadt vor dem Anstieg zum Pass.")
        normalized.contains("смотровая точка над бормио") -> localized("Смотровая точка над Бормио у исторических терм; вид на долину Адды.", "Viewpoint above Bormio by the historic baths, overlooking the Adda valley.", "Mirador sobre Bormio junto a las termas históricas, con vistas al valle del Adda.", "Aussichtspunkt über Bormio bei den historischen Thermen mit Blick ins Adda-Tal.")
        normalized.contains("один из самых высоких перевалов альп") -> localized("Один из самых высоких перевалов Альп (2757 м): серпантины, ледниковые склоны и большая обзорная площадка.", "One of the highest Alpine passes (2,757 m), with hairpin bends, glacial slopes, and a large viewpoint.", "Uno de los puertos alpinos más altos (2757 m), con curvas cerradas, laderas glaciares y un gran mirador.", "Einer der höchsten Alpenpässe (2757 m) mit Serpentinen, Gletscherhängen und großem Aussichtspunkt.")
        normalized.contains("сердце старого города: готическая") -> localized("Сердце Старого города: готическая ратуша и знаменитые астрономические часы.", "The heart of the Old Town: Gothic town hall and famous astronomical clock.", "El corazón del casco antiguo: ayuntamiento gótico y famoso reloj astronómico.", "Das Herz der Altstadt: gotisches Rathaus und berühmte astronomische Uhr.")
        normalized.contains("исторический иезуитский комплекс") -> localized("Исторический иезуитский комплекс с барочной библиотекой и башней с видом на центр.", "Historic Jesuit complex with a Baroque library and a tower overlooking the city center.", "Complejo jesuita histórico con biblioteca barroca y torre con vistas al centro.", "Historischer Jesuitenkomplex mit barocker Bibliothek und Turm mit Blick auf das Stadtzentrum.")
        normalized.contains("каменный мост xiv века") -> localized("Каменный мост XIV века со статуями и панорамой Влтавы.", "14th-century stone bridge with statues and views of the Vltava.", "Puente de piedra del siglo XIV con estatuas y vistas del Moldava.", "Steinbrücke aus dem 14. Jahrhundert mit Statuen und Blick auf die Moldau.")
        normalized.contains("барочная площадь в малой стране") -> localized("Барочная площадь в Малой Стране у подножия Пражского града.", "Baroque square in Malá Strana below Prague Castle.", "Plaza barroca en Malá Strana, al pie del Castillo de Praga.", "Barocker Platz in der Kleinseite am Fuß der Prager Burg.")
        else -> null
    }
    if (known != null) return known
    return when (normalized) {
    "оживлённая площадь у западного входа в исторический центр мюнхена." -> localized("Оживлённая площадь у западного входа в исторический центр Мюнхена.", "Lively square at the western entrance to Munich's historic center.", "Plaza animada en la entrada oeste del centro histórico de Múnich.", "Belebter Platz am westlichen Eingang zur Münchner Altstadt.")
    "пешеходная улица с рождественскими витринами, гирляндами и праздничными украшениями." -> localized("Пешеходная улица с рождественскими витринами, гирляндами и праздничными украшениями.", "Pedestrian street with Christmas shop windows, garlands, and festive decorations.", "Calle peatonal con escaparates navideños, guirnaldas y adornos festivos.", "Fußgängerzone mit weihnachtlichen Schaufenstern, Girlanden und festlicher Dekoration.")
    "средневековые городские ворота, открывающие путь в старый город." -> localized("Средневековые городские ворота, открывающие путь в Старый город.", "Medieval city gates leading to the Old Town.", "Puertas medievales que conducen al casco antiguo.", "Mittelalterliches Stadttor zum Eingang in die Altstadt.")
    "главная площадь мюнхена и сердце праздничного старого города." -> localized("Главная площадь Мюнхена и сердце праздничного Старого города.", "Munich's main square and the heart of the festive Old Town.", "La plaza principal de Múnich y el corazón del casco antiguo festivo.", "Münchens Hauptplatz und das Herz der festlichen Altstadt.")
    "неоготическая ратуша с башней, часами и знаменитым глокеншпилем." -> localized("Неоготическая ратуша с башней, часами и знаменитым Глокеншпилем.", "Neo-Gothic town hall with a tower, clocks, and the famous Glockenspiel.", "Ayuntamiento neogótico con torre, relojes y el famoso Glockenspiel.", "Neugotisches Rathaus mit Turm, Uhr und dem berühmten Glockenspiel.")
    "главная рождественская ярмарка города с ремесленными лавками и баварскими угощениями." -> localized("Главная рождественская ярмарка города с ремесленными лавками и баварскими угощениями.", "The city's main Christmas market with craft stalls and Bavarian treats.", "El principal mercado navideño de la ciudad, con puestos de artesanía y especialidades bávaras.", "Der wichtigste Weihnachtsmarkt der Stadt mit Handwerksständen und bayerischen Spezialitäten.")
    "кафедральный собор и один из главных архитектурных символов мюнхена." -> localized("Кафедральный собор и один из главных архитектурных символов Мюнхена.", "A cathedral and one of Munich's main architectural landmarks.", "Catedral y uno de los principales símbolos arquitectónicos de Múnich.", "Kathedrale und eines der wichtigsten architektonischen Wahrzeichen Münchens.")
    "праздничная торговая улица, особенно красивая в вечерней подсветке." -> localized("Праздничная торговая улица, особенно красивая в вечерней подсветке.", "Festive shopping street, especially beautiful in the evening lights.", "Calle comercial festiva, especialmente bonita con la iluminación nocturna.", "Festliche Einkaufsstraße, besonders schön in der Abendbeleuchtung.")
    "уютная рождественская деревня во дворе мюнхенской резиденции." -> localized("Уютная рождественская деревня во дворе Мюнхенской резиденции.", "Cozy Christmas village in the courtyard of the Munich Residence.", "Pueblo navideño acogedor en el patio de la Residencia de Múnich.", "Behagliches Weihnachtsdorf im Innenhof der Münchner Residenz.")
    "парадная площадь перед баварской государственной оперой и резиденцией." -> localized("Парадная площадь перед Баварской государственной оперой и Резиденцией.", "Grand square in front of the Bavarian State Opera and the Residence.", "Plaza monumental frente a la Ópera Estatal de Baviera y la Residencia.", "Prachtplatz vor der Bayerischen Staatsoper und der Residenz.")
    "монументальная площадь на границе старого города и дворцового квартала." -> localized("Монументальная площадь на границе Старого города и дворцового квартала.", "Monumental square on the edge of the Old Town and palace district.", "Plaza monumental entre el casco antiguo y el barrio de los palacios.", "Monumentaler Platz am Rand der Altstadt und des Residenzviertels.")
    "аркада xix века, вдохновлённая флорентийской лоджией ланци." -> localized("Аркада XIX века, вдохновлённая флорентийской Лоджией Ланци.", "19th-century arcade inspired by Florence's Loggia dei Lanzi.", "Galería del siglo XIX inspirada en la Loggia dei Lanzi de Florencia.", "Arkade aus dem 19. Jahrhundert, inspiriert von der florentinischen Loggia dei Lanzi.")
    "барочная церковь с выразительным жёлтым фасадом и красивой вечерней подсветкой." -> localized("Барочная церковь с выразительным жёлтым фасадом и красивой вечерней подсветкой.", "Baroque church with a striking yellow façade and beautiful evening lighting.", "Iglesia barroca con una llamativa fachada amarilla y una hermosa iluminación nocturna.", "Barocke Kirche mit markanter gelber Fassade und schöner Abendbeleuchtung.")
    "спокойный придворный сад рядом с резиденцией, завершающий прогулку." -> localized("Спокойный придворный сад рядом с Резиденцией, завершающий прогулку.", "Peaceful court garden next to the Residence, the perfect end to the walk.", "Jardín cortesano tranquilo junto a la Residencia, un cierre perfecto para el paseo.", "Ruhiger Hofgarten neben der Residenz als schöner Abschluss des Spaziergangs.")
    else -> localizedSightNameByTerms(value)
}
}

@Composable
internal fun localizedKnownSightDescription(name: String): String? = when (normalizeCatalogText(name)) {
    "ravensburger spieleland" -> localized(
        "Семейный парк развлечений недалеко от Равенсбурга с тематическими зонами и аттракционами для детей.",
        "Family theme park near Ravensburg with themed areas and attractions for children.",
        "Parque familiar cerca de Ravensburg con zonas temáticas y atracciones para niños.",
        "Familienfreizeitpark nahe Ravensburg mit Themenwelten und Attraktionen für Kinder.",
    )
    "museum ravensburger" -> localized(
        "Интерактивный музей истории Ravensburger: игры, книги, пазлы и процесс их создания.",
        "Interactive museum about Ravensburger's history, games, books, puzzles, and how they are made.",
        "Museo interactivo sobre la historia de Ravensburger, sus juegos, libros, puzles y su creación.",
        "Interaktives Museum zur Geschichte von Ravensburger, Spielen, Büchern, Puzzles und ihrer Entstehung.",
    )
    "mehlsack" -> localized(
        "Белая башня около 1425 года и один из главных символов Равенсбурга.",
        "White tower built around 1425 and one of Ravensburg's main landmarks.",
        "Torre blanca construida hacia 1425 y uno de los principales monumentos de Ravensburg.",
        "Um 1425 erbauter weißer Turm und eines der wichtigsten Wahrzeichen Ravensburgs.",
    )
    "grüner turm", "gruener turm" -> localized(
        "Башня начала XV века с зелёной глазурованной черепицей, сохранившейся со времени постройки.",
        "Early-15th-century tower with glazed green roof tiles preserved from its construction.",
        "Torre de principios del siglo XV con tejas vidriadas verdes conservadas desde su construcción.",
        "Turm aus dem frühen 15. Jahrhundert mit seit dem Bau erhaltenen grün glasierten Dachziegeln.",
    )
    "humpis-quartier", "museum humpis-quartier" -> localized(
        "Культурно-исторический музей в пяти зданиях позднесредневекового квартала с тысячелетней историей города.",
        "Cultural history museum in five late-medieval buildings tracing a thousand years of city history.",
        "Museo de historia cultural en cinco edificios tardomedievales que recorren mil años de historia urbana.",
        "Kulturhistorisches Museum in fünf spätmittelalterlichen Gebäuden mit 1.000 Jahren Stadtgeschichte.",
    )
    "замок нойшванштайн", "neuschwanstein", "neuschwanstein castle", "schloss neuschwanstein" -> localized(
        "Замок Людвига II XIX века в баварских Альпах, вдохновлённый образами средневековых замков.",
        "19th-century castle of Ludwig II in the Bavarian Alps, inspired by visions of medieval castles.",
        "Castillo del siglo XIX de Luis II en los Alpes bávaros, inspirado en imágenes de castillos medievales.",
        "Schloss Ludwigs II. aus dem 19. Jahrhundert in den Bayerischen Alpen, inspiriert von mittelalterlichen Burgen.",
    )
    "wirtschaftsmuseum", "wirtschaftsmuseum ravensburg" -> localized(
        "Музей экономической истории региона XIX и XX веков с оригинальными экспонатами и историями людей.",
        "Museum of the region's economic history in the 19th and 20th centuries, with original objects and stories.",
        "Museo de la historia económica regional de los siglos XIX y XX, con objetos originales y relatos.",
        "Museum zur Wirtschaftsgeschichte der Region im 19. und 20. Jahrhundert mit Originalobjekten und Geschichten.",
    )
    else -> null
}

@Composable
internal fun localizedSightInfo(name: String, description: String, category: String): String {
    if (!isPlaceholderSightDescription(description)) return localizedSightDescription(description)
    return localizedKnownSightDescription(name) ?: localizedSightCategory(category)
}

