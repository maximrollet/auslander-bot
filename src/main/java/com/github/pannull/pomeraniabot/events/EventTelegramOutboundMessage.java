package com.github.pannull.pomeraniabot.events;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EventTelegramOutboundMessage extends ApplicationEvent {

    long chatId;
    String message;

    public EventTelegramOutboundMessage(Object source, long chatId, String message) {
        super(source);
        this.chatId = chatId;
        this.message = message;
    }
}
