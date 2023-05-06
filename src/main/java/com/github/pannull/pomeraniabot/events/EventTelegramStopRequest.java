package com.github.pannull.pomeraniabot.events;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EventTelegramStopRequest extends ApplicationEvent {

    long chatId;

    public EventTelegramStopRequest(Object source, long chatId) {
        super(source);
        this.chatId = chatId;
    }
}
