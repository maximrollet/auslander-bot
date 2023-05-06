package com.github.pannull.pomeraniabot.telegram.flows;

import com.github.pannull.pomeraniabot.timeslots.monitoring.TimeSlotMonitoringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TelegramFlowFactory {

    private final ApplicationEventPublisher publisher;
    private final TimeSlotMonitoringRepository monitoringsRepository;
    //private final TimeSlotAvailabilityRepository availabilityRepository;

    public TelegramFlowNewMonitor getNewMonitorFlow(final long chatId) {
        return new TelegramFlowNewMonitor(chatId, this.publisher, this.monitoringsRepository);
    }

    public TelegramFlowDeleteMonitor getDeleteMonitorFlow(final long chatId) {
        return new TelegramFlowDeleteMonitor(chatId, this.publisher, this.monitoringsRepository);
    }
}
