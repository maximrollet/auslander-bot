package com.github.pannull.pomeraniabot.telegram.flows;

import com.github.pannull.pomeraniabot.events.EventFlowCompleted;
import com.github.pannull.pomeraniabot.events.EventTelegramOutboundMessage;
import org.springframework.context.ApplicationEventPublisher;

public abstract class TelegramFlow {

    protected final ApplicationEventPublisher publisher;
    protected final long chatId;

    public abstract void doNext(String input);
    public abstract boolean isCompleted();

    protected TelegramFlow(final long chatId, final ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        this.chatId = chatId;
    }

    protected void sendMessage(final String message) {
        this.publisher.publishEvent(new EventTelegramOutboundMessage(this, this.chatId, message));
    }

    protected void completeFlow() {
        this.publisher.publishEvent(new EventFlowCompleted(this, this.chatId));
    }
}
