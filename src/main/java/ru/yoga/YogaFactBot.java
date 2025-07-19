package ru.yoga;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class YogaFactBot extends TelegramLongPollingBot {
    private final UserHistory userHistory = new UserHistory();
    private final JSONArray yogaFacts;
    private final ConcurrentHashMap<Long, Boolean> activeUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> userCurrentFactIndex = new ConcurrentHashMap<>();
    private final String botToken;
    private final Set<Long> adminIds;

    public YogaFactBot() {
        Properties props = loadProperties();
        this.botToken = props.getProperty("bot.token");
        this.adminIds = Collections.singleton(Long.parseLong(props.getProperty("admin.id")));

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("yoga_facts.json")) {
            if (inputStream == null) {
                throw new RuntimeException("Файл yoga_facts.json не найден в ресурсах");
            }

            // Явное указание кодировки UTF-8 и чтение всего содержимого
            String content = new Scanner(inputStream, StandardCharsets.UTF_8.name())
                    .useDelimiter("\\A").next();

            // Удаление BOM маркера если есть
            content = content.replace("\uFEFF", "");
            System.out.println("Загружен JSON:\n" + content.substring(0, Math.min(100, content.length())));

            this.yogaFacts = new JSONArray(new JSONTokener(content));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки фактов", e);
        }
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Не найден файл конфигурации config.properties");
            }
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки конфигурации", e);
        }
        return props;
    }

    @Override
    public String getBotUsername() {
        return "YogaFactBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            registerUser(chatId);

            if (update.getMessage().getText().equals("/start")) {
                userCurrentFactIndex.put(chatId, 0);
                sendMessage(chatId, "\uD83D\uDE4F Намасте.\n" +
                        "Рады разделить с вами мудрость йоги. Каждое утро — новое знание для практики.");
                sendCurrentFact(chatId);
            } else {
                sendCurrentFact(chatId);
            }
        }
    }

    public void registerUser(long chatId) {
        activeUsers.put(chatId, true);
        userCurrentFactIndex.putIfAbsent(chatId, 0);
    }

    public Set<Long> getAllUsers() {
        return activeUsers.keySet();
    }

    public void sendCurrentFact(long chatId) {
        int currentIndex = userCurrentFactIndex.getOrDefault(chatId, 0);

        if (currentIndex >= yogaFacts.length()) {
            sendMessage(chatId, "Поздравляю! Вы изучили все факты о йоге! 🎉\n" +
                    "Нажмите /start, чтобы начать сначала.");
            return;
        }

        JSONObject fact = yogaFacts.getJSONObject(currentIndex);
        String messageText = "🧘 **Факт дня** (#" + (currentIndex + 1) + "):\n\n" +
                escapeMarkdown(fact.getString("fact")) + "\n\n" +
                "💡 " + escapeMarkdown(fact.getString("benefit")) + "\n" +
                "✏️ " + escapeMarkdown(fact.getString("action"));

        sendMessage(chatId, messageText);
        userCurrentFactIndex.put(chatId, currentIndex + 1);
    }

    private String escapeMarkdown(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}");
    }

    private void sendMessage(long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setParseMode("MarkdownV2");

            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());

            // Попытка отправить без форматирования
            try {
                SendMessage plainMessage = new SendMessage();
                plainMessage.setChatId(chatId);
                plainMessage.setText(text.replace("\\", ""));
                execute(plainMessage);
            } catch (TelegramApiException ex) {
                System.err.println("Ошибка при повторной отправке: " + ex.getMessage());
            }
        }
    }

    public void sendDailyFactToAllUsers() {
        getAllUsers().forEach(chatId -> {
            if (shouldSendFactToday(chatId)) {
                sendCurrentFact(chatId);
            }
        });
    }

    private boolean shouldSendFactToday(long chatId) {
        return true;
    }
}