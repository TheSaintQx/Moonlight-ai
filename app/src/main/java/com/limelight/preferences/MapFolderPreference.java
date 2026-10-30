// Путь: app/src/main/java/com/limelight/preferences/MapFolderPreference.java
// НОВЫЙ файл. Строка «Карта для оверлея» в настройках: тап открывает
// системный выбор папки (SAF, без прав на хранилище), статус показывается
// в подстроке строки. Аналог KbLoaderPreference.
package com.limelight.preferences;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.limelight.assistant.AssistantMap;

import java.lang.ref.WeakReference;

public class MapFolderPreference extends Preference {
    private static WeakReference<Preference> lastInstance = new WeakReference<>(null);

    public MapFolderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public MapFolderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapFolderPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setPersistent(false);
    }

    @Override
    protected void onClick() {
        lastInstance = new WeakReference<>(this);
        AssistantMap.pick(getContext());
    }

    @Override
    public CharSequence getSummary() {
        Context ctx = getContext();
        String name = PreferenceManager.getDefaultSharedPreferences(ctx)
                .getString(AssistantMap.KEY_FOLDER_NAME, "");
        String status = PreferenceManager.getDefaultSharedPreferences(ctx)
                .getString(AssistantMap.KEY_STATUS, "");
        if (status.isEmpty()) {
            return "Тап - выбрать папку с картой (tsushima_map_data.json + картинка)";
        }
        return (name.isEmpty() ? "" : name + ": ") + status;
    }

    public static void notifyStatusChanged() {
        Preference p = lastInstance.get();
        if (p != null) {
            try {
                p.setSummary(p.getSummary());
            } catch (Throwable ignored) {
            }
        }
    }
}
