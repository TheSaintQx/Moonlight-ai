// Путь: app/src/main/java/com/limelight/assistant/AssistantMap.java
// НОВЫЙ файл. Офлайн-карта для оверлея: пользователь выбирает папку с картой
// (tsushima_map_data.json + tsushima.png) в проводнике (SAF, без прав на
// хранилище). Урн сохраняется персистентно — survives restarts.
//
// Структура папки:
//   <папка>/tsushima_map_data.json  — {"image":"tsushima.png","points":[...]}
//   <папка>/<image>                 — картинка карты
//
// При смене игры достаточно выбрать другую папку в настройках.
package com.limelight.assistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;

public final class AssistantMap {
    private static final String TAG = "AssistantMap";
    public static final int REQUEST_CODE = 4712;
    public static final String KEY_FOLDER_URI = "[redacted]";
    public static final String KEY_FOLDER_NAME = "[redacted]";
    public static final String KEY_STATUS = "[redacted]";

    private AssistantMap() {
    }

    public static boolean hasSaved(Context context) {
        return !prefs(context).getString(KEY_FOLDER_URI, "").isEmpty();
    }

    // Вызов из MapFolderPreference.onClick: системный выбор папки.
    public static void pick(Context context) {
        if (!(context instanceof Activity)) {
            setStatus(context, "выбор папки недоступен с этого экрана");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE);
    }

    // Вызов из StreamSettings.onActivityResult().
    public static void handleActivityResult(Context context, int requestCode,
                                            int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        apply(context, data.getData());
    }

    private static void apply(Context context, Uri uri) {
        String name = displayName(uri);
        prefs(context).edit()
                .putString(KEY_FOLDER_URI, uri.toString())
                .putString(KEY_FOLDER_NAME, name)
                .apply();
        // takePersistableUriPermission — иначе ссылка умрёт после перезагрузки
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        // Проверяем, что в папке есть карта
        String check = inspectFolder(context, uri);
        setStatus(context, "папка: " + name + " — " + check);
    }

    /** Проверяем, что в папке есть карта. @return человекочитаемый статус. */
    public static String inspectFolder(Context context, Uri folderUri) {
        try {
            DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
            if (folder == null) {
                return "не удалось открыть папку";
            }
            DocumentFile jsonFile = folder.findFile("tsushima_map_data.json");
            if (jsonFile == null || !jsonFile.exists()) {
                return "tsushima_map_data.json не найден в папке";
            }
            JSONObject data = readJson(context, jsonFile.getUri());
            if (data == null) {
                return "JSON повреждён или пуст";
            }
            int n = data.optJSONArray("points") != null
                    ? data.optJSONArray("points").length() : 0;
            String img = data.optString("image", "tsushima.png");
            return n + " точек, картинка " + img;
        } catch (Exception e) {
            return "ошибка чтения: " + e.getClass().getSimpleName();
        }
    }

    /** Читаем JSON-карту из папки. @return null, если файла нет. */
    public static JSONObject readMapJson(Context context) {
        String uriStr = prefs(context).getString(KEY_FOLDER_URI, "");
        if (uriStr.isEmpty()) return null;
        Uri folderUri = Uri.parse(uriStr);
        // Сначала пробуем DocumentFile (правильный tree-доступ)
        DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
        if (folder != null) {
            DocumentFile jsonFile = folder.findFile("tsushima_map_data.json");
            if (jsonFile != null && jsonFile.exists()) {
                return readJson(context, jsonFile.getUri());
            }
        }
        // Fallback: прямой URI append
        Uri jsonUri = Uri.withAppendedPath(folderUri, "tsushima_map_data.json");
        return readJson(context, jsonUri);
    }

    private static JSONObject readJson(Context context, Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                sb.append(new String(buf, 0, n));
            }
        } catch (Exception e) {
            Log.w(TAG, "readJson failed: " + e.getMessage());
            return null;
        }
        if (sb.length() == 0) return null;
        try {
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static String displayName(Uri uri) {
        String last = uri.getLastPathSegment();
        if (last != null && !last.isEmpty()) return last;
        String doc = uri.getQueryParameter("documentUri");
        return doc == null ? "папка карты" : doc;
    }

    static void setStatus(Context context, String status) {
        Log.i(TAG, status);
        prefs(context).edit().putString(KEY_STATUS, status).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
