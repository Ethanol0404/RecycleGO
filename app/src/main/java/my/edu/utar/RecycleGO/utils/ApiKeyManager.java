package my.edu.utar.RecycleGO.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import my.edu.utar.RecycleGO.BuildConfig;

public class ApiKeyManager {
    private static final String PREFS_NAME = "ApiKeyPrefs";
    private static final String KEY_INDEX = "api_key_index";
    
    private static final String[] API_KEYS;
    
    static {
        String keysStr = BuildConfig.GEMINI_API_KEY;
        if (keysStr == null || keysStr.trim().isEmpty()) {
            API_KEYS = new String[0];
        } else {
            String[] split = keysStr.split(",");
            List<String> list = new ArrayList<>();
            for (String k : split) {
                if (k != null) {
                    // Trim whitespace AND remove any literal double quotes
                    String cleanedKey = k.trim().replace("\"", "");
                    if (!cleanedKey.isEmpty()) {
                        list.add(cleanedKey);
                    }
                }
            }
            API_KEYS = list.toArray(new String[0]);
        }
    }

    public static String getCurrentKey(Context context) {
        if (API_KEYS.length == 0) {
            return "";
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int index = prefs.getInt(KEY_INDEX, 0);
        
        if (index < 0 || index >= API_KEYS.length) {
            index = 0;
            prefs.edit().putInt(KEY_INDEX, 0).apply();
        }
        return API_KEYS[index];
    }

    public static void rotateKey(Context context) {
        if (API_KEYS.length <= 1) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentIndex = prefs.getInt(KEY_INDEX, 0);
        int nextIndex = (currentIndex + 1) % API_KEYS.length;
        prefs.edit().putInt(KEY_INDEX, nextIndex).apply();
    }

    public static int getApiKeysCount() {
        return API_KEYS.length;
    }
}
