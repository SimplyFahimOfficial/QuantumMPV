/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.browser.folderlist

import com.quantummpv.app.utils.storage.FileTypeUtils
import java.io.File

/** What happened to one folder; a folder is never counted together with the files inside it. */
internal enum class FolderDeleteOutcome { DELETED, PARTIAL, FAILED }

/** Folder-level (deleted, failed) counts for the shared delete toast; a partial folder counts in both. */
internal fun List<FolderDeleteOutcome>.toDeleteCounts(): Pair<Int, Int> =
  count { it != FolderDeleteOutcome.FAILED } to count { it != FolderDeleteOutcome.DELETED }

/** Deletes the media files directly inside [dir] (blocking; call off the main thread). */
internal fun deleteMediaFilesIn(
  dir: File,
  includeVideo: Boolean,
  includeAudio: Boolean,
): FolderDeleteOutcome {
  val matching =
    dir.listFiles()?.filter { file ->
      val ext = file.extension.lowercase()
      file.isFile &&
        ((includeVideo && ext in FileTypeUtils.VIDEO_EXTENSIONS) || (includeAudio && ext in FileTypeUtils.AUDIO_EXTENSIONS))
    }
  if (matching.isNullOrEmpty()) return FolderDeleteOutcome.FAILED
  val deleted = matching.count { it.delete() }
  return when (deleted) {
    matching.size -> FolderDeleteOutcome.DELETED
    0 -> FolderDeleteOutcome.FAILED
    else -> FolderDeleteOutcome.PARTIAL
  }
}

/** Outcome of a delete-everything request given the media-file result and whether [dir] is gone. */
internal fun wholeFolderOutcome(
  filesDeleted: Int,
  filesFailed: Int,
  dirRemoved: Boolean,
): FolderDeleteOutcome =
  when {
    filesFailed == 0 && dirRemoved -> FolderDeleteOutcome.DELETED
    filesDeleted > 0 || dirRemoved -> FolderDeleteOutcome.PARTIAL
    else -> FolderDeleteOutcome.FAILED
  }
