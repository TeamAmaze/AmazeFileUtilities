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

import java.util.ArrayList;
import java.util.List;

import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.PlaylistsColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PlaylistLoader {

  @NonNull
  public static List<MediaFileInfo.Playlist> getAllPlaylists(@NonNull final Context context) {
    return getAllPlaylists(makePlaylistCursor(context, null, null));
  }

  @NonNull
  public static MediaFileInfo.Playlist getPlaylist(
      @NonNull final Context context, final long playlistId) {
    return getPlaylist(
        makePlaylistCursor(
            context, BaseColumns._ID + "=?", new String[] {String.valueOf(playlistId)}));
  }

  @NonNull
  public static MediaFileInfo.Playlist getPlaylist(
      @NonNull final Context context, final String playlistName) {
    return getPlaylist(
        makePlaylistCursor(context, PlaylistsColumns.NAME + "=?", new String[] {playlistName}));
  }

  @NonNull
  public static MediaFileInfo.Playlist getPlaylist(@Nullable final Cursor cursor) {
    MediaFileInfo.Playlist playlist = new MediaFileInfo.Playlist(-1, "");

    if (cursor != null && cursor.moveToFirst()) {
      playlist = getPlaylistFromCursorImpl(cursor);
    }
    if (cursor != null) cursor.close();
    return playlist;
  }

  @NonNull
  public static List<MediaFileInfo.Playlist> getAllPlaylists(@Nullable final Cursor cursor) {
    List<MediaFileInfo.Playlist> playlists = new ArrayList<>();

    if (cursor != null && cursor.moveToFirst()) {
      do {
        playlists.add(getPlaylistFromCursorImpl(cursor));
      } while (cursor.moveToNext());
    }
    if (cursor != null) cursor.close();
    return playlists;
  }

  @NonNull
  private static MediaFileInfo.Playlist getPlaylistFromCursorImpl(@NonNull final Cursor cursor) {
    final long id = cursor.getLong(0);
    final String name = cursor.getString(1);
    return new MediaFileInfo.Playlist(id, name);
  }

  @Nullable
  public static Cursor makePlaylistCursor(
      @NonNull final Context context, final String selection, final String[] values) {
    try {
      return context
          .getContentResolver()
          .query(
              MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
              new String[] {
                /* 0 */
                BaseColumns._ID,
                /* 1 */
                PlaylistsColumns.NAME
              },
              selection,
              values,
              MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
    } catch (SecurityException e) {
      return null;
    }
  }
}
