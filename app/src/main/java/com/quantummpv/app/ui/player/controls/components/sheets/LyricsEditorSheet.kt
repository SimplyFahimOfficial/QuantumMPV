/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.quantummpv.app.ui.player.controls.components.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quantummpv.app.R
import com.quantummpv.app.domain.lyrics.LyricsDraft
import com.quantummpv.app.domain.lyrics.LyricsDraftError
import com.quantummpv.app.domain.lyrics.LyricsSourceType
import com.quantummpv.app.presentation.components.ConfirmDialog
import com.quantummpv.app.ui.icons.Icon
import com.quantummpv.app.ui.icons.Icons
import com.quantummpv.app.ui.player.PlaybackPhase
import com.quantummpv.app.ui.player.PlaybackSession
import com.quantummpv.app.ui.player.PlayerViewModel
import com.quantummpv.app.ui.player.controls.components.rememberSmoothedPositionMs
import com.quantummpv.app.ui.theme.fontFamilyForText
import com.quantummpv.app.utils.media.LyricsUtils
import kotlinx.coroutines.flow.update
import kotlin.math.roundToLong

/** Opens the lyric editor for the track that is playing. */
@Composable
fun LyricsEditAction(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.lyricsUiState.collectAsStateWithLifecycle()
  val editState by viewModel.lyricsEditUiState.collectAsStateWithLifecycle()
  val playback by PlaybackSession.state.collectAsStateWithLifecycle()
  val enabled =
    !state.isLoading && !editState.isSaving && playback.currentItem?.audiobook == null &&
      playback.phase in setOf(PlaybackPhase.READY, PlaybackPhase.BACKGROUND)
  IconButton(onClick = viewModel::openLyricsEditor, enabled = enabled, modifier = modifier) {
    Icon(Icons.RoundedFilled.Edit, contentDescription = stringResource(R.string.lyrics_editor_title))
  }
}

/**
 * Full-screen editor for the lyrics of one track. Edits are saved in the app's private storage
 * and replace the track's lyrics; the audio file is never touched.
 */
@Composable
fun LyricsEditorSheet(
  viewModel: PlayerViewModel,
  onDismiss: () -> Unit,
) {
  val editTarget by viewModel.lyricsEditTarget.collectAsStateWithLifecycle()
  val target = editTarget
  if (target == null) {
    LaunchedEffect(Unit) { onDismiss() }
    return
  }

  val initialDraft = remember(target) { LyricsDraft.from(target.lyrics) }
  val draft by viewModel.lyricsEditDraft.collectAsStateWithLifecycle()
  var selected by rememberSaveable(target.mediaPath) {
    mutableIntStateOf(initialDraft.lines.indexOfFirst { it.timeMs == null }.coerceAtLeast(0))
  }
  var validationError by rememberSaveable { mutableStateOf<LyricsDraftError?>(null) }
  var discardPrompt by rememberSaveable { mutableStateOf(false) }
  var restorePrompt by rememberSaveable { mutableStateOf(false) }
  var clearTimesPrompt by rememberSaveable { mutableStateOf(false) }
  var pasteDialog by rememberSaveable { mutableStateOf(false) }
  var pasteText by rememberSaveable { mutableStateOf("") }
  var pasteError by rememberSaveable { mutableStateOf(false) }
  var timeIndex by rememberSaveable { mutableStateOf<Int?>(null) }
  var timeText by rememberSaveable { mutableStateOf("") }
  var timeError by rememberSaveable { mutableStateOf(false) }
  var optionsExpanded by remember { mutableStateOf(false) }
  val editState by viewModel.lyricsEditUiState.collectAsStateWithLifecycle()
  val playback by PlaybackSession.state.collectAsStateWithLifecycle()
  val rawPosition by viewModel.precisePosition.collectAsStateWithLifecycle()
  val paused by PlaybackSession.propBoolean["pause"].collectAsStateWithLifecycle()
  val speed by PlaybackSession.propFloat["speed"].collectAsStateWithLifecycle()
  val seekable by PlaybackSession.propBoolean["seekable"].collectAsStateWithLifecycle()
  val context = LocalContext.current
  val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
  val listState = rememberLazyListState()
  val changed = remember(draft, initialDraft) { !draft.hasSameContent(initialDraft) }
  val sameTrack = viewModel.isLyricsEditTrackCurrent(target)
  val playbackReady = playback.phase in setOf(PlaybackPhase.READY, PlaybackPhase.BACKGROUND)
  val canTime = sameTrack && playbackReady && rawPosition.isFinite() && !editState.isSaving
  val positionMs by rememberSmoothedPositionMs(
    rawPositionMs = if (rawPosition.isFinite()) (rawPosition * 1000.0).roundToLong().coerceAtLeast(0) else 0,
    isPlaying = paused == false && playbackReady,
    speed = speed ?: 1f,
  )
  val singingLine by remember(draft, sameTrack) {
    derivedStateOf {
      if (sameTrack) draft.lines.indexOfLast { line -> line.timeMs?.let { it <= positionMs } == true } else -1
    }
  }
  val successMessage =
    stringResource(if (editState.restoredOriginal) R.string.lyrics_editor_restored else R.string.lyrics_editor_saved)
  val requestClose: () -> Unit = {
    when {
      editState.isSaving -> Unit
      changed -> discardPrompt = true
      else -> onDismiss()
    }
  }

  LaunchedEffect(editState.completed) {
    if (editState.completed) {
      Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
      onDismiss()
    }
  }
  LaunchedEffect(selected, draft.lines.size) {
    runCatching { listState.animateScrollToItem(selected.coerceIn(0, draft.lines.lastIndex)) }
  }

  Dialog(
    onDismissRequest = requestClose,
    properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
  ) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
      Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = requestClose, enabled = !editState.isSaving) {
            Icon(Icons.RoundedFilled.Close, contentDescription = stringResource(R.string.lyrics_editor_cancel))
          }
          Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.lyrics_editor_title), style = MaterialTheme.typography.titleMedium)
            Text(
              target.title,
              fontFamily = fontFamilyForText(target.title),
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Box {
            IconButton(onClick = { optionsExpanded = true }, enabled = !editState.isSaving) {
              Icon(Icons.RoundedFilled.MoreVert, contentDescription = stringResource(R.string.lyrics_editor_options))
            }
            DropdownMenu(expanded = optionsExpanded, onDismissRequest = { optionsExpanded = false }) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.lyrics_editor_paste)) },
                onClick = {
                  optionsExpanded = false
                  pasteDialog = true
                },
              )
              DropdownMenuItem(
                text = { Text(stringResource(R.string.lyrics_editor_clear_times)) },
                enabled = draft.lines.any { it.timeMs != null },
                onClick = {
                  optionsExpanded = false
                  clearTimesPrompt = true
                },
              )
              if (target.hasSavedEdits) {
                DropdownMenuItem(
                  text = {
                    Text(stringResource(R.string.lyrics_editor_restore_title), color = MaterialTheme.colorScheme.error)
                  },
                  onClick = {
                    optionsExpanded = false
                    restorePrompt = true
                  },
                )
              }
            }
          }
        }
        if (!keyboardVisible) {
          Text(
            stringResource(R.string.lyrics_editor_instructions),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (!sameTrack) {
          Text(
            stringResource(R.string.lyrics_editor_track_changed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        validationError?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        editState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(
          state = listState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          itemsIndexed(draft.lines, key = { _, line -> line.id }) { index, line ->
            var lineMenu by remember { mutableStateOf(false) }
            val lineLabel = stringResource(R.string.lyrics_editor_line, index + 1)
            val isSelected = selected == index
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
              modifier = Modifier.clickable(enabled = !editState.isSaving) { selected = index },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TextButton(
                  enabled = !editState.isSaving,
                  modifier =
                    Modifier
                      .widthIn(min = 78.dp, max = 104.dp)
                      .background(
                        if (singingLine == index) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        MaterialTheme.shapes.small,
                      ),
                  onClick = {
                    selected = index
                    if (canTime && seekable == true && line.timeMs != null) viewModel.seekToLyricTime(line.timeMs)
                  },
                ) {
                  Text(
                    line.timeMs?.let { LyricsDraft.formatTime(it) } ?: stringResource(R.string.lyrics_editor_untimed),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                  )
                }
                BasicTextField(
                  value = line.text,
                  onValueChange = { text ->
                    viewModel.lyricsEditDraft.update { it.editText(index, text) }
                    validationError = null
                  },
                  enabled = !editState.isSaving,
                  maxLines = if (isSelected) 4 else 2,
                  cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                  textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                      fontFamily = fontFamilyForText(line.text),
                      color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    ),
                  modifier =
                    Modifier
                      .weight(1f)
                      .heightIn(min = 48.dp)
                      .padding(horizontal = 8.dp, vertical = 12.dp)
                      .onFocusChanged { if (it.isFocused) selected = index }
                      .semantics { contentDescription = lineLabel },
                  decorationBox = { field ->
                    Box {
                      if (line.text.isEmpty()) {
                        Text(lineLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      field()
                    }
                  },
                )
                Box {
                  IconButton(
                    onClick = {
                      selected = index
                      lineMenu = true
                    },
                    enabled = !editState.isSaving,
                  ) {
                    Icon(
                      Icons.RoundedFilled.MoreVert,
                      contentDescription = stringResource(R.string.lyrics_editor_line_options, index + 1),
                    )
                  }
                  DropdownMenu(expanded = lineMenu, onDismissRequest = { lineMenu = false }) {
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.lyrics_editor_edit_time)) },
                      onClick = {
                        lineMenu = false
                        timeIndex = index
                        timeText = line.timeMs?.let { LyricsDraft.formatTime(it) }.orEmpty()
                        timeError = false
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.lyrics_editor_remove_time)) },
                      enabled = line.timeMs != null,
                      onClick = {
                        lineMenu = false
                        viewModel.lyricsEditDraft.update { it.setTime(index, null) }
                        validationError = null
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.lyrics_editor_insert_line)) },
                      onClick = {
                        lineMenu = false
                        viewModel.lyricsEditDraft.update { it.insertAfter(index) }
                        selected = index + 1
                        validationError = null
                      },
                    )
                    DropdownMenuItem(
                      text = { Text(stringResource(R.string.lyrics_editor_delete_line), color = MaterialTheme.colorScheme.error) },
                      onClick = {
                        lineMenu = false
                        val updated = viewModel.lyricsEditDraft.value.delete(index)
                        viewModel.lyricsEditDraft.value = updated
                        selected = selected.coerceAtMost(updated.lines.lastIndex)
                        validationError = null
                      },
                    )
                  }
                }
              }
            }
          }
        }

        if (!keyboardVisible) {
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = canTime, onClick = { if (viewModel.isLyricsEditTrackCurrent(target)) viewModel.pauseUnpause() }) {
              Icon(
                if (paused == false) Icons.RoundedFilled.Pause else Icons.RoundedFilled.PlayArrow,
                contentDescription = stringResource(if (paused == false) R.string.lyrics_editor_pause else R.string.lyrics_editor_play),
              )
            }
            Text(
              LyricsDraft.formatTime(positionMs.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()),
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
              enabled = !editState.isSaving,
              onClick = {
                viewModel.lyricsEditDraft.update { it.insertAfter(selected) }
                selected++
                validationError = null
              },
            ) {
              Icon(Icons.RoundedFilled.Add, contentDescription = stringResource(R.string.lyrics_editor_insert_line))
            }
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val selectedTime = draft.lines.getOrNull(selected)?.timeMs
            FilledTonalButton(
              modifier = Modifier.weight(1f),
              enabled = selectedTime != null && !editState.isSaving,
              onClick = {
                val current = viewModel.lyricsEditDraft.value.lines[selected].timeMs
                if (current == null) {
                  validationError = LyricsDraftError.INCOMPLETE_TIMING
                } else {
                  viewModel.lyricsEditDraft.update { it.setTime(selected, (current - 100).coerceAtLeast(0)) }
                  validationError = null
                }
              },
            ) { Text(stringResource(R.string.lyrics_editor_earlier)) }
            Button(
              modifier = Modifier.weight(1.6f),
              enabled = canTime,
              onClick = {
                if (viewModel.isLyricsEditTrackCurrent(target)) {
                  val time = ((positionMs + 5) / 10 * 10).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                  viewModel.lyricsEditDraft.update { it.setTime(selected, time) }
                  if (selected < viewModel.lyricsEditDraft.value.lines.lastIndex) selected++
                  validationError = null
                }
              },
            ) { Text(stringResource(R.string.lyrics_editor_set_time)) }
            FilledTonalButton(
              modifier = Modifier.weight(1f),
              enabled = selectedTime != null && !editState.isSaving,
              onClick = {
                val current = viewModel.lyricsEditDraft.value.lines[selected].timeMs
                if (current == null) {
                  validationError = LyricsDraftError.INCOMPLETE_TIMING
                } else {
                  val time = (current.toLong() + 100).coerceAtMost(Int.MAX_VALUE.toLong())
                  viewModel.lyricsEditDraft.update { it.setTime(selected, time.toInt()) }
                  validationError = null
                }
              },
            ) { Text(stringResource(R.string.lyrics_editor_later)) }
          }
          Text(
            stringResource(R.string.lyrics_editor_storage),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(onClick = requestClose, enabled = !editState.isSaving) { Text(stringResource(R.string.lyrics_editor_cancel)) }
          Button(
            enabled = changed && !editState.isSaving,
            onClick = {
              val snapshot = viewModel.lyricsEditDraft.value
              validationError = snapshot.validationError()
              if (validationError == null) viewModel.saveEditedLyrics(snapshot)
            },
          ) {
            if (editState.isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Text(stringResource(R.string.lyrics_editor_save))
            }
          }
        }
      }
    }
  }

  if (discardPrompt) {
    ConfirmDialog(
      title = stringResource(R.string.lyrics_editor_discard_title),
      subtitle = stringResource(R.string.lyrics_editor_discard_message),
      onConfirm = {
        discardPrompt = false
        onDismiss()
      },
      onCancel = { discardPrompt = false },
    )
  }
  if (restorePrompt) {
    ConfirmDialog(
      title = stringResource(R.string.lyrics_editor_restore_title),
      subtitle = stringResource(R.string.lyrics_editor_restore_message),
      onConfirm = {
        restorePrompt = false
        viewModel.restoreOriginalLyrics()
      },
      onCancel = { restorePrompt = false },
    )
  }
  if (clearTimesPrompt) {
    ConfirmDialog(
      title = stringResource(R.string.lyrics_editor_clear_times),
      subtitle = stringResource(R.string.lyrics_editor_clear_times_message),
      onConfirm = {
        clearTimesPrompt = false
        viewModel.lyricsEditDraft.update { it.clearTimes() }
        validationError = null
      },
      onCancel = { clearTimesPrompt = false },
    )
  }
  if (pasteDialog) {
    AlertDialog(
      onDismissRequest = { pasteDialog = false },
      title = { Text(stringResource(R.string.lyrics_editor_paste)) },
      text = {
        OutlinedTextField(
          value = pasteText,
          onValueChange = {
            pasteText = it
            pasteError = false
          },
          modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
          label = { Text(stringResource(R.string.lyrics_editor_paste_hint)) },
          isError = pasteError,
          supportingText = { if (pasteError) Text(stringResource(R.string.lyrics_editor_paste_empty)) },
        )
      },
      dismissButton = { TextButton(onClick = { pasteDialog = false }) { Text(stringResource(R.string.lyrics_editor_cancel)) } },
      confirmButton = {
        TextButton(
          onClick = {
            val imported = LyricsUtils.parseLyrics(pasteText, LyricsSourceType.LOCAL)
            if (!imported.isValid()) {
              pasteError = true
            } else {
              viewModel.lyricsEditDraft.value = LyricsDraft.from(imported)
              selected = 0
              validationError = null
              pasteDialog = false
              pasteText = ""
            }
          },
        ) { Text(stringResource(R.string.lyrics_editor_confirm)) }
      },
    )
  }
  timeIndex?.let { index ->
    AlertDialog(
      onDismissRequest = { timeIndex = null },
      title = { Text(stringResource(R.string.lyrics_editor_edit_time)) },
      text = {
        OutlinedTextField(
          value = timeText,
          onValueChange = {
            if (it.length <= 64) {
              timeText = it
              timeError = false
            } else {
              timeError = true
            }
          },
          singleLine = true,
          label = { Text(stringResource(R.string.lyrics_editor_time_hint)) },
          isError = timeError,
          supportingText = { if (timeError) Text(stringResource(R.string.lyrics_editor_time_invalid)) },
        )
      },
      dismissButton = { TextButton(onClick = { timeIndex = null }) { Text(stringResource(R.string.lyrics_editor_cancel)) } },
      confirmButton = {
        TextButton(
          onClick = {
            val time = LyricsDraft.parseTime(timeText)
            if (time == null) {
              timeError = true
            } else {
              viewModel.lyricsEditDraft.update { it.setTime(index, time) }
              validationError = null
              timeIndex = null
            }
          },
        ) { Text(stringResource(R.string.lyrics_editor_confirm)) }
      },
    )
  }
}

@androidx.annotation.StringRes
private fun LyricsDraftError.messageRes(): Int =
  when (this) {
    LyricsDraftError.EMPTY -> R.string.lyrics_editor_empty
    LyricsDraftError.INCOMPLETE_TIMING -> R.string.lyrics_editor_incomplete_timing
    LyricsDraftError.OUT_OF_ORDER -> R.string.lyrics_editor_out_of_order
    LyricsDraftError.INVALID_TIME -> R.string.lyrics_editor_time_invalid
  }
