package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;
    private final NotificationTaskRepository notificationTaskRepository;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // dividing the received text into a date and quest
            Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
            Matcher matcher = pattern.matcher(update.message().text());
            String date = null;
            String item = null;

            Long chatId = update.message().chat().id();
            //
            if (update.message().text().equals("/start")) {
                SendMessage message = new SendMessage(chatId, String.format("Привет, %s ! Введи задачу в формтае: 01.01.0001 00:00, Введите Текст задачи: ...", update.message().from().firstName()));
                telegramBot.execute(message);

            } else if (matcher.matches()) {
                date = matcher.group(1);
                item = matcher.group(3);
                logger.info("Date: {}, item: {}", date, item);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                if (date != null) {
                    LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
                    notificationTaskRepository.save(new NotificationTask(chatId, item, dateTime));
                    SendMessage message = new SendMessage(chatId, "Задача сохранена!");
                    telegramBot.execute(message);
                }

            } else {
                SendMessage message = new SendMessage(chatId, "неверный формат задачи");
                telegramBot.execute(message);
            }
        });

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}

