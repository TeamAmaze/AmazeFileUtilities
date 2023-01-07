/*
 * Copyright (C) 2021-2023 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Utilities.
 *
 * Amaze File Utilities is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.fileutilities.audio_player.playlist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo;

public class M3UWriter {

  private static final String EXTENSION = "m3u";
  private static final String HEADER = "#EXTM3U";
  private static final String ENTRY = "#EXTINF:";
  private static final String DURATION_SEPARATOR = ",";

  public static File write(File dir, MediaFileInfo.Playlist playlist, List<MediaFileInfo> songs)
      throws IOException {
    if (!dir.exists()) // noinspection ResultOfMethodCallIgnored
    dir.mkdirs();
    File file = new File(dir, playlist.getName().concat("." + EXTENSION));

    if (songs.size() > 0) {
      BufferedWriter bw = new BufferedWriter(new FileWriter(file));

      bw.write(HEADER);
      for (MediaFileInfo song : songs) {
        if (song.getExtraInfo() != null && song.getExtraInfo().getAudioMetaData() != null) {
          MediaFileInfo.AudioMetaData metaData = song.getExtraInfo().getAudioMetaData();
          bw.newLine();
          bw.write(
              ENTRY
                  + metaData.getDuration()
                  + DURATION_SEPARATOR
                  + metaData.getArtistName()
                  + " - "
                  + song.getTitle());
          bw.newLine();
          bw.write(song.getPath());
        }
      }

      bw.close();
    }

    return file;
  }
}
