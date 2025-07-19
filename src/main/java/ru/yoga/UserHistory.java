package ru.yoga;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserHistory {
    private static final String FILE_PATH = "user_history.json";
    private final Map<Long, Set<Integer>> userSentFacts = new ConcurrentHashMap<>();
    private final Object fileLock = new Object();

    public UserHistory() {
        loadFromFile();
    }

    private void loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        synchronized (fileLock) {
            try (FileReader reader = new FileReader(file)) {
                JSONObject json = new JSONObject(new JSONTokener(reader));
                json.keySet().forEach(chatId -> {
                    JSONArray facts = json.getJSONArray(chatId);
                    Set<Integer> sentFacts = ConcurrentHashMap.newKeySet();
                    facts.forEach(fact -> sentFacts.add((Integer) fact));
                    userSentFacts.put(Long.parseLong(chatId), sentFacts);
                });
            } catch (Exception e) {
                System.err.println("Ошибка загрузки истории: " + e.getMessage());
            }
        }
    }

    public boolean isFactSent(long chatId, int factId) {
        return userSentFacts.getOrDefault(chatId, Collections.emptySet()).contains(factId);
    }

    public void addFact(long chatId, int factId) {
        userSentFacts.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(factId);
        saveToFile();
    }

    public void clearUserHistory(long chatId) {
        userSentFacts.remove(chatId);
        saveToFile();
    }

    private void saveToFile() {
        synchronized (fileLock) {
            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                JSONObject json = new JSONObject();
                userSentFacts.forEach((chatId, facts) ->
                        json.put(String.valueOf(chatId), new JSONArray(facts)));
                writer.write(json.toString(2));  // Pretty print
            } catch (Exception e) {
                System.err.println("Ошибка сохранения истории: " + e.getMessage());
            }
        }
    }

    public Set<Long> getAllUsers() {
        return userSentFacts.keySet();
    }
}