package com.sendbird.desk.android.sample.utils;

import android.app.DownloadManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Hashtable;

/**
 * DateUtils related to file handling (for sending / downloading file messages).
 */
public class FileUtils {

    private FileUtils() {
    }

    public static Hashtable<String, Object> getFileInfo(final Context context, final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    Hashtable<String, Object> value = new Hashtable<>();
                    value.put("path", Environment.getExternalStorageDirectory() + "/" + split[1]);
                    final String path = (String) value.get("path");
                    if (path != null) {
                        value.put("size", (int) new File(path).length());
                    }
                    value.put("mime", "application/octet-stream");

                    return value;
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Hashtable<String, Object> value = new Hashtable<>();
            value.put("path", uri.getPath());
            final String path = (String) value.get("path");
            if (path != null) {
                value.put("size", (int) new File(path).length());
            }
            value.put("mime", "application/octet-stream");

            return value;
        }

        return null;
    }

    private static Hashtable<String, Object> getDataColumn(Context context, Uri uri, String selection,
                                                           String[] selectionArgs) {

        Cursor cursor = null;
        String COLUMN_DATA = MediaStore.MediaColumns.DATA;
        String COLUMN_MIME = MediaStore.MediaColumns.MIME_TYPE;
        String COLUMN_SIZE = MediaStore.MediaColumns.SIZE;

        String[] projection = {
                COLUMN_DATA,
                COLUMN_MIME,
                COLUMN_SIZE
        };

        try {
            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                        null);
            } catch (IllegalArgumentException e) {
                COLUMN_MIME = "mimetype"; // DownloadProvider.sAppReadableColumnsArray.COLUMN_MIME_TYPE
                projection[1] = COLUMN_MIME;
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                        null);
            }

            if (cursor != null && cursor.moveToFirst()) {
                String path = "";
                String mime = "application/octet-stream";
                int size = 0;

                int column_index = cursor.getColumnIndex(COLUMN_DATA);
                if (column_index != -1) {
                    path = cursor.getString(column_index);
                }

                column_index = cursor.getColumnIndex(COLUMN_MIME);
                if (column_index != -1) {
                    mime = cursor.getString(column_index);
                }

                column_index = cursor.getColumnIndex(COLUMN_SIZE);
                if (column_index != -1) {
                    size = cursor.getInt(column_index);
                }

                Hashtable<String, Object> value = new Hashtable<>();
                value.put("path", path);
                value.put("mime", mime);
                value.put("size", size);

                return value;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Downloads a file using DownloadManager.
     */
    public static void downloadFile(Context context, String url, String fileName) {
        DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(url));
        downloadRequest.setTitle(fileName);

        // in order for this if to run, you must use the android 3.2 to compile your app
        downloadRequest.allowScanningByMediaScanner();
        downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(downloadRequest);
    }


    /**
     * Converts byte value to String.
     */
    public static String toReadableFileSize(long size) {
        String fileSizeString = "0 KB";
        try {
            if (size > 0) {
                final String[] units = new String[] { "B", "KB", "MB", "GB", "TB"} ;
                int digitGroups = (int)(Math.log10(size) / Math.log10(1024));
                fileSizeString = new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSizeString;
    }

}
