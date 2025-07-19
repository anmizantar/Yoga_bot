package ru.yoga;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Calendar;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        YogaFactBot bot = new YogaFactBot();
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);


        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


        long initialDelay = calculateInitialDelay();


        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Отправка ежедневных фактов...");
                bot.sendDailyFactToAllUsers();
            } catch (Exception e) {
                System.err.println("Ошибка при отправке ежедневных фактов: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelay, TimeUnit.DAYS.toSeconds(24), TimeUnit.SECONDS);

        System.out.println("Бот запущен и готов к работе!");
    }


    private static long calculateInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        // Устанавливаем время следующего запуска на 9:00
        nextRun.set(Calendar.HOUR_OF_DAY, 9);
        nextRun.set(Calendar.MINUTE, 0);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);


        if (now.after(nextRun)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        return (nextRun.getTimeInMillis() - now.getTimeInMillis()) / 1000;
    }
}