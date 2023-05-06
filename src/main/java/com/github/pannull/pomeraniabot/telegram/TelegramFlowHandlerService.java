package com.github.pannull.pomeraniabot.telegram;

import com.github.pannull.pomeraniabot.events.*;
import com.github.pannull.pomeraniabot.telegram.flows.TelegramFlow;
import com.github.pannull.pomeraniabot.telegram.flows.TelegramFlowFactory;
import com.github.pannull.pomeraniabot.timeslots.availability.TimeSlotAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TelegramFlowHandlerService {

    private final TimeSlotAvailabilityRepository availabilityRepository;
    private final ApplicationEventPublisher publisher;
    private final TelegramFlowFactory flowFactory;
    private final HashMap<Long,TelegramFlow> runningFlows = new HashMap<>();

    @EventListener
    public void handleTelegramStartRequest(final EventTelegramStartRequest event) {
        val flow = this.flowFactory.getNewMonitorFlow(event.getChatId());
        this.runningFlows.put(event.getChatId(), flow);
        flow.doNext(null); // input is null because it's the first step
    }

    @EventListener
    public void handleTelegramStopRequest(final EventTelegramStopRequest event) {
        val flow = this.flowFactory.getDeleteMonitorFlow(event.getChatId());
        this.runningFlows.put(event.getChatId(), flow);
        flow.doNext(null); // input is null because it's the first step
    }

    @EventListener
    public void handleTelegramCheckRequest(final EventTelegramCheckRequest event) {
        if (this.availabilityRepository.count() == 0) {
            this.publisher.publishEvent(new EventTelegramOutboundMessage(this, event.getChatId(), "No time slots available."));
        } else {
            val availability = this.availabilityRepository.findAll();
            val message = new StringBuilder("Available time slots:\n\n");
            availability.forEach(slot -> message.append(String.format("%s --> %s\n", slot.getDate(), slot.getTime())));
            message.append("\nPlease note that this list is not updated in real time. It is updated every few minutes.\n\n");
            message.append("Make reservation: https://kolejkagdansk.ajhmedia.pl/branch/5");
            this.publisher.publishEvent(new EventTelegramOutboundMessage(this, event.getChatId(), message.toString()));
        }
    }

    @EventListener
    public void handleTelegramHelpRequest(final EventTelegramHelpRequest event) {
        this.publisher.publishEvent(new EventTelegramOutboundMessage(this, event.getChatId(),
    """
            Available commands:
            
            /start - start a new monitoring
            /stop  - stop an existing monitoring
            /check - show currently available time slots
            /help  - show this help message
            
            Please note that currently you can only have one monitoring per chat.
            
            Support (although not guaranteed): @pannull
            """
        ));
    }



    @EventListener
    public void handleFlowCompleted(final EventFlowCompleted event) {
        log.info("Removing flow for chatId {} from cache.", event.getChatId());
        this.runningFlows.remove(event.getChatId());
    }

    @EventListener
    public void handleInboundTelegramMessage(final EventTelegramInboundMessage event) {
        if (this.runningFlows.containsKey(event.getChatId()) && !this.runningFlows.get(event.getChatId()).isCompleted()) {
            val flow = this.runningFlows.get(event.getChatId());
            flow.doNext(event.getMessage());
        } else {
            this.publisher.publishEvent(new EventTelegramOutboundMessage(this, event.getChatId(), "Please see /help for available commands."));
        }
    }
}