package com.github.pannull.pomeraniabot.telegram;


import com.github.pannull.pomeraniabot.events.*;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TelegramUserHandlerService{

    private final ApplicationEventPublisher publisher;
    private final TelegramBot telegramBot;

    public TelegramUserHandlerService (@Value("${monitor.telegram-token}") String telegramToken, @Autowired ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        this.telegramBot = new TelegramBot(telegramToken);
        this.telegramBot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null) {
                    handleInboundMessage(update.message().chat().id(), update.message().text());
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void handleInboundMessage(final long chatId, final String message) {
        switch (message) {
            case "/start" -> this.publisher.publishEvent(new EventTelegramStartRequest(this, chatId));
            case "/stop" -> this.publisher.publishEvent(new EventTelegramStopRequest(this, chatId));
            case "/check" -> this.publisher.publishEvent(new EventTelegramCheckRequest(this, chatId));
            case "/help" -> this.publisher.publishEvent(new EventTelegramHelpRequest(this, chatId));
            default -> this.publisher.publishEvent(new EventTelegramInboundMessage(this, chatId, message));
        }
    }

    @EventListener
    public void handleOutboundMessage(final EventTelegramOutboundMessage event) {
        this.telegramBot.execute(new SendMessage(event.getChatId(), event.getMessage()));
    }
}
