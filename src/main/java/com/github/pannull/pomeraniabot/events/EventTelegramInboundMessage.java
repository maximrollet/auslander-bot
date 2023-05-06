package com.github.pannull.pomeraniabot.events;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EventTelegramInboundMessage extends ApplicationEvent {

    long chatId;
    String message;

    public EventTelegramInboundMessage(Object source, long chatId, String message) {
        super(source);
        this.chatId = chatId;
        this.message = message;
    }
}
