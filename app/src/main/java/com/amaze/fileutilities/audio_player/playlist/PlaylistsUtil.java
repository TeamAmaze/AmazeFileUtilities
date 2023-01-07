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

import static android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amaze.fileutilities.R;
import com.amaze.fileutilities.home_page.ui.files.MediaFileInfo;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** @author Karim Abou Zeid (kabouzeid) */
public class PlaylistsUtil {

  private static final Logger LOG = LoggerFactory.getLogger(PlaylistsUtil.class);

  public static boolean doesPlaylistExist(@NonNull final Context context, final long playlistId) {
    return playlistId != -1
        && doesPlaylistExist(
            context,
            MediaStore.Audio.Playlists._ID + "=?",
            new String[] {String.valueOf(playlistId)});
  }

  public static boolean doesPlaylistExist(@NonNull final Context context, final String name) {
    return doesPlaylistExist(
        context, MediaStore.Audio.PlaylistsColumns.NAME + "=?", new String[] {name});
  }

  public static long createPlaylist(@NonNull final Context context, @Nullable final String name) {
    LOG.info("create playlist with name " + name);
    long id = -1;
    if (name != null && name.length() > 0) {
      try {
        Cursor cursor =
            context
                .getContentResolver()
                .query(
                    EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Playlists._ID},
                    MediaStore.Audio.PlaylistsColumns.NAME + "=?",
                    new String[] {name},
                    null);
        if (cursor == null || cursor.getCount() < 1) {
          final ContentValues values = new ContentValues(1);
          values.put(MediaStore.Audio.PlaylistsColumns.NAME, name);
          final Uri uri = context.getContentResolver().insert(EXTERNAL_CONTENT_URI, values);
          if (uri != null) {
            // Necessary because somehow the MediaStoreObserver doesn't work for playlists
            context.getContentResolver().notifyChange(uri, null);
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.created_playlist),
                    Toast.LENGTH_SHORT)
                .show();
            id = Long.parseLong(uri.getLastPathSegment());
          }
        } else {
          // Playlist exists
          int columnIdx = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID);
          if (cursor.moveToFirst() && columnIdx >= 0) {
            id = cursor.getLong(columnIdx);
          }
        }
        if (cursor != null) {
          cursor.close();
        }
      } catch (SecurityException se) {
        LOG.warn("exception while saving playlist " + name, se);
      }
    }
    if (id == -1) {
      Toast.makeText(
              context,
              context.getResources().getString(R.string.could_not_create_playlist),
              Toast.LENGTH_SHORT)
          .show();
    }
    return id;
  }

  public static void deletePlaylists(
      @NonNull final Context context, @NonNull final List<MediaFileInfo.Playlist> playlists) {
    final StringBuilder selection = new StringBuilder();
    selection.append(MediaStore.Audio.Playlists._ID + " IN (");
    for (int i = 0; i < playlists.size(); i++) {
      selection.append(playlists.get(i).getId());
      if (i < playlists.size() - 1) {
        selection.append(",");
      }
    }
    selection.append(")");
    try {
      context.getContentResolver().delete(EXTERNAL_CONTENT_URI, selection.toString(), null);
      // Necessary because somehow the MediaStoreObserver doesn't work for playlists
      context.getContentResolver().notifyChange(EXTERNAL_CONTENT_URI, null);
    } catch (SecurityException se) {
      LOG.warn("failed to delete playlists", se);
    }
  }

  public static void addToPlaylist(
      @NonNull final Context context,
      final MediaFileInfo mediaFile,
      final long playlistId,
      final boolean showToastOnFinish) {
    List<MediaFileInfo> helperList = new ArrayList<>();
    helperList.add(mediaFile);
    addToPlaylist(context, helperList, playlistId, showToastOnFinish);
  }

  public static void addToPlaylist(
      @NonNull final Context context,
      @NonNull final List<MediaFileInfo> songs,
      final long playlistId,
      final boolean showToastOnFinish) {
    final ContentResolver resolver = context.getContentResolver();
    final String[] projection =
        new String[] {
          "max(" + MediaStore.Audio.Playlists.Members.PLAY_ORDER + ")",
        };
    final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
    Cursor cursor = null;
    int base = 0;

    try {
      try {
        cursor = resolver.query(uri, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
          base = cursor.getInt(0) + 1;
        }
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
      List<Integer> songIds = new ArrayList<>();
      for (MediaFileInfo file : songs) {
        songIds.add((int) file.getId());
      }

      int numInserted = resolver.bulkInsert(uri, makeInsertItems(songIds, base));

      // Necessary because somehow the MediaStoreObserver doesn't work for playlists
      context.getContentResolver().notifyChange(uri, null);

      if (showToastOnFinish) {
        Toast.makeText(
                context,
                context
                    .getResources()
                    .getString(
                        R.string.insert_songs_playlist,
                        numInserted,
                        getNameForPlaylist(context, playlistId)),
                Toast.LENGTH_SHORT)
            .show();
      }
    } catch (SecurityException se) {
      LOG.warn("failed to add to playlist due to security exception", se);
      Toast.makeText(
              context,
              context.getResources().getString(R.string.failed_insert_songs_playlist),
              Toast.LENGTH_SHORT)
          .show();
    } catch (Exception e) {
      LOG.warn("failed to add to playlist", e);
      Toast.makeText(
              context,
              context.getResources().getString(R.string.failed_insert_songs_playlist),
              Toast.LENGTH_SHORT)
          .show();
    }
  }

  @NonNull
  public static ContentValues[] makeInsertItems(
      @NonNull final List<Integer> songIds, final int base) {
    ContentValues[] contentValues = new ContentValues[songIds.size()];

    for (int i = 0; i < songIds.size(); i++) {
      contentValues[i] = new ContentValues();
      contentValues[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + i);
      contentValues[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songIds.get(i));
    }
    return contentValues;
  }

  public static void removeFromPlaylist(
      @NonNull final Context context, @NonNull final List<MediaFileInfo> songs) {
    for (MediaFileInfo song : songs) {
      if (song.getExtraInfo() != null
          && song.getExtraInfo().getAudioMetaData() != null
          && song.getExtraInfo().getAudioMetaData().getPlaylist() != null) {
        long playlistId = song.getExtraInfo().getAudioMetaData().getPlaylist().getId();
        removeSongFromPlaylist(context, song, playlistId);
      }
    }
  }

  private static void removeSongFromPlaylist(
      @NonNull final Context context, @NonNull final MediaFileInfo song, long playlistId) {
    Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
    String selection = MediaStore.Audio.Playlists.Members.AUDIO_ID + " =?";
    try {
      String[] selectionArgs = new String[] {String.valueOf(song.getId())};
      context.getContentResolver().delete(uri, selection, selectionArgs);
      // Necessary because somehow the MediaStoreObserver doesn't work for playlists
      context.getContentResolver().notifyChange(uri, null);
      Toast.makeText(
              context,
              context.getResources().getString(R.string.removed_from_playlist),
              Toast.LENGTH_SHORT)
          .show();
    } catch (SecurityException se) {
      LOG.warn("failed to remove from playlist due to security exception", se);
      Toast.makeText(
              context,
              context.getResources().getString(R.string.failed_remove_songs_playlist),
              Toast.LENGTH_SHORT)
          .show();
    } catch (Exception e) {
      LOG.warn("failed to remove from playlist", e);
      Toast.makeText(
              context,
              context.getResources().getString(R.string.failed_remove_songs_playlist),
              Toast.LENGTH_SHORT)
          .show();
    }
  }

  /*public static void removeFromPlaylist(@NonNull final Context context, @NonNull final List<PlaylistSong> songs) {
      final long playlistId = songs.get(0).playlistId;
      Uri uri = MediaStore.Audio.Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId);
      String selectionArgs[] = new String[songs.size()];
      for (int i = 0; i < selectionArgs.length; i++) {
          selectionArgs[i] = String.valueOf(songs.get(i).idInPlayList);
      }
      String selection = MediaStore.Audio.Playlists.Members._ID + " in (";
      //noinspection unused
      for (String selectionArg : selectionArgs) selection += "?, ";
      selection = selection.substring(0, selection.length() - 2) + ")";

      try {
          context.getContentResolver().delete(uri, selection, selectionArgs);
          // Necessary because somehow the MediaStoreObserver is not notified when adding a playlist
          context.getContentResolver().notifyChange(uri, null);
      } catch (SecurityException ignored) {
      }
  }*/

  public static boolean doesPlaylistContain(
      @NonNull final Context context, final long playlistId, final long songId) {
    if (playlistId != -1) {
      try {
        Cursor c =
            context
                .getContentResolver()
                .query(
                    MediaStore.Audio.Playlists.Members.getContentUri(
                        MediaStore.VOLUME_EXTERNAL, playlistId),
                    new String[] {MediaStore.Audio.Playlists.Members.AUDIO_ID},
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?",
                    new String[] {String.valueOf(songId)},
                    null);
        int count = 0;
        if (c != null) {
          count = c.getCount();
          c.close();
        }
        return count > 0;
      } catch (SecurityException se) {
        LOG.warn("failed to get playlist items", se);
      }
    }
    return false;
  }

  public static boolean moveItem(
      @NonNull final Context context, long playlistId, int from, int to) {
    boolean res =
        MediaStore.Audio.Playlists.Members.moveItem(
            context.getContentResolver(), playlistId, from, to);
    // Necessary because somehow the MediaStoreObserver doesn't work for playlists
    // NOTE: actually for now lets disable this because it messes with the animation (tested on
    // Android 11)
    //
    // context.getContentResolver().notifyChange(ContentUris.withAppendedId(EXTERNAL_CONTENT_URI,
    // playlistId), null);
    return res;
  }

  public static void renamePlaylist(
      @NonNull final Context context, final long id, final String newName) {
    Uri playlistUri = ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id);
    ContentValues contentValues = new ContentValues();
    contentValues.put(MediaStore.Audio.PlaylistsColumns.NAME, newName);
    try {
      context.getContentResolver().update(playlistUri, contentValues, null, null);

      // Necessary because somehow the MediaStoreObserver doesn't work for playlists
      context.getContentResolver().notifyChange(playlistUri, null);
    } catch (SecurityException se) {
      LOG.warn("failed to rename playlist", se);
    }
  }

  public static String getNameForPlaylist(@NonNull final Context context, final long id) {
    try {
      Cursor cursor =
          context
              .getContentResolver()
              .query(
                  ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id),
                  new String[] {MediaStore.Audio.PlaylistsColumns.NAME},
                  null,
                  null,
                  null);
      if (cursor != null) {
        try (cursor) {
          if (cursor.moveToFirst()) {
            return cursor.getString(0);
          }
        }
      }
    } catch (SecurityException se) {
      LOG.warn("failed to get playlist name", se);
    }
    return "";
  }

  public static File savePlaylist(MediaFileInfo.Playlist playlist, List<MediaFileInfo> songs)
      throws IOException {
    return M3UWriter.write(
        new File(Environment.getExternalStorageDirectory(), "Playlists"), playlist, songs);
  }

  private static boolean doesPlaylistExist(
      @NonNull Context context, @NonNull final String selection, @NonNull final String[] values) {
    Cursor cursor =
        context
            .getContentResolver()
            .query(EXTERNAL_CONTENT_URI, new String[] {}, selection, values, null);

    boolean exists = false;
    if (cursor != null) {
      exists = cursor.getCount() != 0;
      cursor.close();
    }
    return exists;
  }
}
