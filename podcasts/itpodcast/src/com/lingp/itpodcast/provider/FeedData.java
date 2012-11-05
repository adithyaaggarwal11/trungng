/**
 * Sparse rss
 * 
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.lingp.itpodcast.provider;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.lingp.itpodcast.handler.PictureFilenameFilter;

public class FeedData {
  public static final String CONTENT = "content://";

  public static final String AUTHORITY = "com.lingp.itpodcast.provider.FeedData";

  private static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY AUTOINCREMENT";
  protected static final String TYPE_TEXT = "TEXT";
  protected static final String TYPE_DATETIME = "DATETIME";
  protected static final String TYPE_INT = "INT";
  protected static final String TYPE_BOOLEAN = "INTEGER(1)";

  public static final String FEED_DEFAULTSORTORDER = SubscriptionColumns.PRIORITY;

  /**
   * Contract data and table for Subscriptions.
   * This is stored at content://authority/feeds.
   */
  public static class SubscriptionColumns implements BaseColumns {

    public static final String URL = "url";
    public static final String NAME = "name";
    public static final String LASTUPDATE = "lastupdate";
    public static final String ICON = "icon";
    public static final String ERROR = "error";
    public static final String PRIORITY = "priority";
    public static final String FETCHMODE = "fetchmode";
    public static final String REALLASTUPDATE = "reallastupdate";
    public static final String WIFIONLY = "wifionly";

    public static final String[] COLUMNS = new String[] { _ID, URL, NAME,
        LASTUPDATE, ICON, ERROR, PRIORITY, FETCHMODE, REALLASTUPDATE, WIFIONLY };

    public static final String[] TYPES = new String[] { TYPE_PRIMARY_KEY,
        "TEXT UNIQUE", TYPE_TEXT, TYPE_DATETIME, "BLOB", TYPE_TEXT, TYPE_INT,
        TYPE_INT, TYPE_DATETIME, TYPE_BOOLEAN };

    /**
     * All subscriptions
     */
    public static final Uri CONTENT_URI = Uri.parse(new StringBuilder(CONTENT)
        .append(AUTHORITY).append("/feeds").toString());
    
    /**
     * A subscription with given ID (input as string)
     * TODO(trung): use withAppendedId
     */
    public static final Uri subscriptionContentUri(String feedId) {
      return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
          .append("/feeds/").append(feedId).toString());
    }

    /**
     * A subscription with given id (input as long)
     * TODO(trung): use withAppendedId
     */
    public static final Uri subscriptionContentUri(long feedId) {
      return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
          .append("/feeds/").append(feedId).toString());
    }
  }

  /**
   * Tables for Feed items. They are stored at content://authority/entries
   * and content://authority/favorites.
   * 
   * TODO(trung): add table for recently viewed (played)
   * content://authority/recent 
   */
  public static class ItemColumns implements BaseColumns {
    public static final String FEED_ID = "feedid";
    public static final String TITLE = "title";
    public static final String ABSTRACT = "abstract";
    public static final String DATE = "date";
    public static final String READDATE = "readdate";
    public static final String LINK = "link";
    public static final String FAVORITE = "favorite";
    public static final String ENCLOSURE = "enclosure";
    public static final String GUID = "guid";

    public static final String[] COLUMNS = new String[] { _ID, FEED_ID, TITLE,
        ABSTRACT, DATE, READDATE, LINK, FAVORITE, ENCLOSURE, GUID };

    public static final String[] TYPES = new String[] { TYPE_PRIMARY_KEY,
        "INTEGER(7)", TYPE_TEXT, TYPE_TEXT, TYPE_DATETIME, TYPE_DATETIME,
        TYPE_TEXT, TYPE_BOOLEAN, TYPE_TEXT, TYPE_TEXT };

    /**
     * All feed items
     */
    public static Uri ALL_ITEMS_CONTENT_URI = Uri.parse(new StringBuilder(CONTENT)
        .append(AUTHORITY).append("/entries").toString());

    /**
     * All favorite feed items
     */
    public static Uri FAVORITES_CONTENT_URI = Uri.parse(new StringBuilder(
        CONTENT).append(AUTHORITY).append("/favorites").toString());

    /**
     * All favorite feed items
     */
    public static Uri RECENTLY_VIEWED_CONTENT_URI = Uri.parse(new StringBuilder(
        CONTENT).append(AUTHORITY).append("/recent").toString());
    
    /**
     * All entries of a particular subscription (feed) with given id
     * TODO(trung): use withAppendedId
     */
    public static Uri subscriptionItemsContentUri(String subscriptionId) {
      return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
          .append("/feeds/").append(subscriptionId).append("/entries").toString());
    }

    /**
     * A feed item
     */
    public static Uri itemContentUri(String itemId) {
      return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
          .append("/entries/").append(itemId).toString());
    }

    public static Uri PARENT_URI(String path) {
      return Uri.parse(new StringBuilder(CONTENT).append(AUTHORITY)
          .append(path.substring(0, path.lastIndexOf('/'))).toString());
    }

  }

  private static String[] IDPROJECTION = new String[] { FeedData.ItemColumns._ID };

  public static void deletePicturesOfFeedAsync(final Context context,
      final Uri entriesUri, final String selection) {
    if (FeedDataContentProvider.IMAGEFOLDER_FILE.exists()) {
      new Thread() {
        public void run() {
          deletePicturesOfFeed(context, entriesUri, selection);
        }
      }.start();
    }
  }

  public static synchronized void deletePicturesOfFeed(Context context,
      Uri entriesUri, String selection) {
    if (FeedDataContentProvider.IMAGEFOLDER_FILE.exists()) {
      PictureFilenameFilter filenameFilter = new PictureFilenameFilter();

      Cursor cursor = context.getContentResolver().query(entriesUri,
          IDPROJECTION, selection, null, null);

      while (cursor.moveToNext()) {
        filenameFilter.setEntryId(cursor.getString(0));

        File[] files = FeedDataContentProvider.IMAGEFOLDER_FILE
            .listFiles(filenameFilter);

        for (int n = 0, i = files != null ? files.length : 0; n < i; n++) {
          files[n].delete();
        }
      }
      cursor.close();
    }
  }

  public static synchronized void deletePicturesOfEntry(String entryId) {
    if (FeedDataContentProvider.IMAGEFOLDER_FILE.exists()) {
      PictureFilenameFilter filenameFilter = new PictureFilenameFilter(entryId);

      File[] files = FeedDataContentProvider.IMAGEFOLDER_FILE
          .listFiles(filenameFilter);

      for (int n = 0, i = files != null ? files.length : 0; n < i; n++) {
        files[n].delete();
      }
    }
  }

}