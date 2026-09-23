package com.furybook.android.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.furybook.android.data.AndroidContentPackState
import com.furybook.android.data.AndroidFcpInstaller
import com.furybook.content.FcpComposition
import com.furybook.dubl.content.DublChiFcp
import com.furybook.dubl.application.CharacterTransferImportResult
import com.furybook.dubl.data.CharacterTransferRejectReason
import com.furybook.android.state.CharacterController
import com.furybook.ui.components.DublCard
import com.furybook.android.ui.components.DublScreenHeader
import java.nio.charset.StandardCharsets

private const val TRANSFER_EXTENSION = ".dubl"

@Composable
fun CharactersScreen(
    controller: CharacterController,
    contentPackComposition: FcpComposition,
    onContentPackActiveChange: (String, Boolean) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var transferStatus by remember { mutableStateOf<String?>(null) }
    var contentPackStatus by remember { mutableStateOf<String?>(null) }
    var installedPackRevision by remember { mutableIntStateOf(0) }
    val snapshot = controller.snapshot
    val context = LocalContext.current
    val displayedComposition = remember(contentPackComposition, installedPackRevision) {
        AndroidContentPackState.composition(
            context,
            contentPackComposition.isActive(DublChiFcp.PACK_ID),
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            transferStatus = if (writeTransferFile(context, uri, controller.exportActiveCharacter())) {
                "Персонаж экспортирован."
            } else {
                "Не удалось сохранить файл персонажа."
            }
        }
    }
    val fcpImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentPackStatus = runCatching {
                val result = AndroidFcpInstaller.install(
                    context = context,
                    uri = uri,
                    reservedPackIds = AndroidContentPackState.bundledPackIds(),
                )
                installedPackRevision += 1
                "Установлен FCP: ${result.manifest.name} v${result.manifest.version}."
            }.getOrElse { error ->
                "Ошибка импорта FCP: ${error.message ?: "неизвестная ошибка"}"
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = readTransferFile(context, uri)
            transferStatus = if (raw == null) {
                "Не удалось прочитать файл персонажа."
            } else {
                importStatus(controller.importCharacter(raw))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        DublScreenHeader(
            title = "Персонажи",
            subtitle = "Выбор активного листа",
            action = {
                Button(onClick = controller::createCharacter) { Text("+ Создать") }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f),
            ) { Text("Импорт") }
            OutlinedButton(
                onClick = {
                    exportLauncher.launch(safeTransferFileName(controller.active.name) + TRANSFER_EXTENSION)
                },
                modifier = Modifier.weight(1f),
            ) { Text("Экспорт") }
        }
        transferStatus?.let { status ->
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DublCard(Modifier.fillMaxWidth()) {
            Text("Fury Content Packs", style = MaterialTheme.typography.titleMedium)
            Text(
                "Активные FCP определяют правила, каталоги и подключаемые части интерфейса.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { fcpImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Импортировать .fcp") }
            displayedComposition.available.forEach { manifest ->
                val required = manifest.id in displayedComposition.requiredPackIds
                val canActivate = AndroidContentPackState.canActivatePack(manifest.id)
                ContentPackRow(
                    name = manifest.name,
                    version = manifest.version,
                    enabled = displayedComposition.isActive(manifest.id),
                    toggleEnabled = !required && canActivate,
                    subtitle = if (required) {
                        "Основной ruleset · обязателен"
                    } else if (!canActivate) {
                        "Установлен · adapter support пока отсутствует"
                    } else if (manifest.dependencies.isEmpty()) {
                        "Опциональный FCP"
                    } else {
                        "Опциональный FCP · зависит от ${manifest.dependencies.joinToString { it.id }}"
                    },
                    onToggle = { enabled ->
                        contentPackStatus = runCatching {
                            onContentPackActiveChange(manifest.id, enabled)
                            if (enabled) "${manifest.name} включён." else "${manifest.name} выключен."
                        }.getOrElse { error ->
                            "Ошибка FCP: ${error.message ?: "неизвестная ошибка"}"
                        }
                    },
                )
            }
            contentPackStatus?.let { status ->
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        snapshot.characters.forEach { character ->
            val active = character.id == snapshot.activeCharacterId
            DublCard(Modifier.fillMaxWidth()) {
                Text(
                    character.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${character.concept.ifBlank { "Без концепта" }} • ${character.experience} опыта",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (active) {
                    Text("Активный", color = MaterialTheme.colorScheme.primary)
                } else {
                    OutlinedButton(onClick = { controller.selectCharacter(character.id) }) {
                        Text("Открыть")
                    }
                }
            }
        }

        if (snapshot.characters.size > 1) {
            OutlinedButton(onClick = { confirmDelete = true }) {
                Text("Удалить активного персонажа")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить персонажа?") },
            text = { Text("${controller.active.name} будет удалён с этого устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    controller.deleteActive()
                    confirmDelete = false
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ContentPackRow(
    name: String,
    version: String,
    enabled: Boolean,
    toggleEnabled: Boolean,
    subtitle: String,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text("$subtitle · v$version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = enabled, enabled = toggleEnabled, onCheckedChange = onToggle)
    }
}

private fun writeTransferFile(context: Context, uri: Uri, raw: String): Boolean = runCatching {
    val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot open output stream")
    stream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(raw) }
}.isSuccess

private fun readTransferFile(context: Context, uri: Uri): String? = runCatching {
    val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open input stream")
    stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}.getOrNull()

private fun importStatus(result: CharacterTransferImportResult): String = when (result) {
    is CharacterTransferImportResult.Imported -> "Импортирован персонаж: ${result.name}."
    is CharacterTransferImportResult.Rejected -> when (result.reason) {
        CharacterTransferRejectReason.INVALID_FILE -> "Это не поддерживаемый файл персонажа DUBL."
        CharacterTransferRejectReason.UNSUPPORTED_FORMAT_VERSION -> "Версия файла персонажа пока не поддерживается."
        CharacterTransferRejectReason.UNSUPPORTED_RULESET -> "Этот файл создан для другого рулбука или версии правил."
    }
}

private fun safeTransferFileName(name: String): String = name
    .trim()
    .ifBlank { "character" }
    .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
    .take(80)
