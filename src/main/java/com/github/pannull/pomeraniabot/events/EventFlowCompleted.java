package com.github.pannull.pomeraniabot.events;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EventFlowCompleted extends ApplicationEvent {

    long chatId;

    public EventFlowCompleted(Object source, long chatId) {
        super(source);
        this.chatId = chatId;
    }
}
