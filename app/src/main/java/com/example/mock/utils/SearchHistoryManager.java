package com.example.mock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.mock.R;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "search_history_prefs";
    private static final String KEY_HISTORY = "history_list";
    private static final int MAX_HISTORY_SIZE = 10;
    
    private SharedPreferences prefs;
    
    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public List<String> getHistory() {
        List<String> history = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                history.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return history;
    }
    
    public void addSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        
        List<String> history = getHistory();
        
        // Remove if already exists to move it to top
        if (history.contains(query)) {
            history.remove(query);
        }
        
        // Add to top
        history.add(0, query);
        
        // Limit size
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        
        saveHistory(history);
    }
    
    public void removeSearch(String query) {
        List<String> history = getHistory();
        if (history.contains(query)) {
            history.remove(query);
            saveHistory(history);
        }
    }
    
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
    
    private void saveHistory(List<String> history) {
        JSONArray array = new JSONArray();
        for (String item : history) {
            array.put(item);
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
    }
}
