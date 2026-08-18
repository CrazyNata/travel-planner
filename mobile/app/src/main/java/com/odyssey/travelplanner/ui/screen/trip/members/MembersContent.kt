package com.odyssey.travelplanner.ui.screen.trip.members

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripOverview
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkTint
import com.odyssey.travelplanner.ui.theme.OdysseyNoFontPadding
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.dangerSurfaceColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun MembersContent(tripId: String, overview: TripOverview, canEdit: Boolean = true, onRoleUpdated: () -> Unit) {
    val language = LocalLanguage.current
    val dashedBorderColor = if (LocalDarkTheme.current) OdysseyDarkBorder else Color(0xFFD3D3DB)
    var savingMemberId by remember { mutableStateOf<String?>(null) }
    var deleteMember by remember { mutableStateOf<com.odyssey.travelplanner.data.TripMember?>(null) }
    var deleteMemberError by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Редактор") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(localized("Участники", "Members", "Participantes", "Teilnehmer"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                if (canEdit) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(tintedSurfaceColor()).clickable { editing = !editing }.padding(horizontal = 13.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = localized("Изменить участников", "Edit members", "Editar participantes", "Teilnehmer bearbeiten"), tint = primaryColor(), modifier = Modifier.size(16.dp))
                        Text(if (editing) localized("Готово", "Done", "Listo", "Fertig") else localized("Изменить", "Edit", "Editar", "Bearbeiten"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp))
                    }
                }
            }
        }
        if (actionMessage != null) {
            item {
                Text(actionMessage!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
            }
        }
        if (overview.members.isEmpty()) {
            item { Text(localized("Участники пока не добавлены", "No members added yet", "Aún no se han añadido participantes", "Noch keine Mitglieder hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            items(overview.members, key = { it.id }) { member ->
                MemberCard(member, savingMemberId == member.id, canEdit && editing, onDelete = {
                    if (canEdit) {
                        deleteMember = member
                        deleteMemberError = null
                    }
                }) { role ->
                    if (canEdit) {
                        scope.launch {
                            savingMemberId = member.id
                            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateMemberRole(tripId, member.id, role) }
                                .onSuccess {
                                    actionMessage = null
                                    onRoleUpdated()
                                }
                                .onFailure {
                                    actionMessage = it.message ?: localized(language, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u0440\u043e\u043b\u044c. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0438 \u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u043e\u043f\u044b\u0442\u043a\u0443.", "Could not change the role. Check your connection and try again.", "No se pudo cambiar el rol. Comprueba la conexi\u00f3n e int\u00e9ntalo de nuevo.", "Die Rolle konnte nicht geändert werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.")
                                }
                            savingMemberId = null
                        }
                    }
                }
            }
        }
        if (canEdit) item {
            if (adding) {
                Column(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428)).clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(horizontal = 15.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InviteMemberField(localized("Имя участника", "Member name", "Nombre", "Name"), name) { name = it }
                    InviteMemberField("e-mail ${localized("нового участника", "of new member", "del nuevo participante", "des neuen Mitglieds")}", email) { email = it }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(secondarySurfaceColor()).padding(3.dp),
                    ) {
                        listOf(
                            localized("Редактор", "Editor", "Editor", "Editor") to "Редактор",
                            localized("Просмотр", "Viewer", "Lector", "Leser") to "Читатель",
                        ).forEach { (label, value) ->
                            val selected = value == role
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (selected) cardSurfaceColor() else Color.Transparent).border(if (selected) 1.dp else 0.dp, if (selected) contentBorderColor() else Color.Transparent, RoundedCornerShape(10.dp)).clickable { role = value }) {
                                Text(label, color = if (selected) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                            }
                        }
                    }
                    if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(47.dp).clip(RoundedCornerShape(13.dp)).background(cardSurfaceColor()).border(1.dp, contentBorderColor(), RoundedCornerShape(13.dp)).clickable { adding = false; message = null }) {
                            Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(47.dp).shadow(6.dp, RoundedCornerShape(13.dp), clip = false, ambientColor = Color(0x476C5CE7), spotColor = Color(0x476C5CE7)).clip(RoundedCornerShape(13.dp)).background(primaryColor()).clickable(enabled = !saving) {
                            scope.launch {
                                saving = true
                                runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addMember(tripId, name, email, role) }
                                    .onSuccess { adding = false; name = ""; email = ""; onRoleUpdated() }
                                    .onFailure { message = it.message ?: localized(language, "Не удалось добавить участника", "Could not add member", "No se pudo añadir al participante", "Mitglied konnte nicht hinzugefügt werden") }
                                saving = false
                            }
                        }) {
                            Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Пригласить", "Invite", "Invitar", "Einladen"), color = primaryContentColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        if (canEdit) item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (LocalDarkTheme.current) secondarySurfaceColor() else Color.White.copy(alpha = 0.4f))
                    .clickable { adding = true }
                    .drawBehind {
                        drawRoundRect(
                            color = dashedBorderColor,
                            cornerRadius = CornerRadius(18.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                        )
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("＋", color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 18.sp)
                    Text(localized("Пригласить участника", "Invite member", "Invitar participante", "Mitglied einladen"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
    }
    if (canEdit) deleteMember?.let { member ->
        val deleting = savingMemberId == member.id
        AlertDialog(
            onDismissRequest = {
                if (!deleting) {
                    deleteMember = null
                    deleteMemberError = null
                }
            },
            title = {
                Text(
                    localized("Удалить участника?", "Remove member?", "¿Eliminar participante?", "Mitglied entfernen?"),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localized(
                            "Участник «${member.name}» будет удалён из путешествия.",
                            "Member “${member.name}” will be removed from this trip.",
                            "El participante «${member.name}» se eliminará de este viaje.",
                            "Das Mitglied „${member.name}“ wird aus dieser Reise entfernt.",
                        ),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W600,
                    )
                    deleteMemberError?.let { error ->
                        Text(error, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteMember = null
                        deleteMemberError = null
                    },
                    enabled = !deleting,
                ) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            savingMemberId = member.id
                            deleteMemberError = null
                            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "members", member.id) }
                                .onSuccess {
                                    deleteMember = null
                                    deleteMemberError = null
                                    actionMessage = null
                                    onRoleUpdated()
                                }
                                .onFailure {
                                    deleteMemberError = it.message ?: localized(language, "Не удалось удалить участника. Проверьте интернет и повторите попытку.", "Could not remove the member. Check your connection and try again.", "No se pudo eliminar al participante. Comprueba la conexión e inténtalo de nuevo.", "Mitglied konnte nicht entfernt werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.")
                                }
                            savingMemberId = null
                        }
                    },
                    enabled = !deleting,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD9534F)),
                ) {
                    Text(
                        if (deleting) localized("Удаляем…", "Removing…", "Eliminando…", "Wird entfernt…")
                        else localized("Удалить", "Remove", "Eliminar", "Entfernen"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
}

@Composable
internal fun InviteMemberField(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .clip(shape)
            .background(cardSurfaceColor())
            .border(1.dp, contentBorderColor(), shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 14.5.sp,
                platformStyle = OdysseyNoFontPadding,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(primaryColor()),
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            placeholder,
                            color = secondaryTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W600,
                            fontSize = 14.5.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
internal fun MemberCard(member: com.odyssey.travelplanner.data.TripMember, saving: Boolean, editing: Boolean, onDelete: () -> Unit, onRoleChange: (String) -> Unit) {
    val language = LocalLanguage.current
    val darkTheme = LocalDarkTheme.current
    val surface = cardSurfaceColor()
    val avatarColor = when (member.tone) {
        "sand", "orange" -> Color(0xFFF29A32)
        "teal", "green" -> Color(0xFF35AEB9)
        else -> primaryColor()
    }
    val isOwner = member.role == "Владелец"
    val roleLabel = when (member.role) {
        "Владелец" -> localized(language, "Владелец", "Owner", "Propietario", "Besitzer")
        "Редактор" -> localized(language, "Редактор", "Editor", "Editor", "Editor")
        "Читатель" -> localized(language, "Просмотр", "Viewer", "Lector", "Leser")
        else -> member.role
    }
    val roleBackground = when (member.role) {
        "Владелец" -> if (darkTheme) OdysseyDarkTint else Color(0xFFEDEAFF)
        "Редактор" -> if (darkTheme) Color(0xFF203C35) else Color(0xFFEEFAF3)
        else -> secondarySurfaceColor()
    }
    val roleColor = when (member.role) {
        "Владелец" -> primaryColor()
        "Редактор" -> Color(0xFF22B07D)
        else -> secondaryTextColor()
    }
    Column(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428)).clip(RoundedCornerShape(18.dp)).background(surface).padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(avatarColor)) {
                Text(member.initials.take(1), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                Text(member.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (member.email.isNotBlank()) Text(member.email, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!editing || isOwner) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(roleBackground).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(if (saving) "…" else roleLabel, color = roleColor, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp)
                }
            }
        }
        if (editing && !isOwner) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                listOf(
                    localized("Редактор", "Editor", "Editor", "Editor") to "Редактор",
                    localized("Просмотр", "Viewer", "Lector", "Leser") to "Читатель",
                ).forEach { (label, value) ->
                    val selected = member.role == value
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) cardSurfaceColor() else secondarySurfaceColor()).border(if (selected) 1.dp else 0.dp, if (selected) contentBorderColor() else Color.Transparent, RoundedCornerShape(11.dp)).clickable(enabled = !saving) { onRoleChange(value) }) {
                        Text(label, color = if (selected) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                    }
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(dangerSurfaceColor()).clickable(enabled = !saving) { onDelete() }) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить участника", "Remove member", "Eliminar participante", "Mitglied entfernen"), tint = Color(0xFFE35D61), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

