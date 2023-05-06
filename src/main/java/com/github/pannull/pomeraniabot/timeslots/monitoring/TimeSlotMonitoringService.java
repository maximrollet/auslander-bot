package com.github.pannull.pomeraniabot.timeslots.monitoring;

import com.github.pannull.pomeraniabot.events.EventTelegramOutboundMessage;
import com.github.pannull.pomeraniabot.timeslots.availability.TimeSlotAvailability;
import com.github.pannull.pomeraniabot.timeslots.availability.TimeSlotAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TimeSlotMonitoringService {

    private final TimeSlotAvailabilityRepository availabilityRepository;
    private final TimeSlotMonitoringRepository monitoringRepository;
    private final ApplicationEventPublisher publisher;

    @Scheduled(fixedRate = 1000 * 60 * 2, initialDelay = 1000 * 60 * 2)
    public void handleMonitoring() {
        log.info("Checking for active monitors...");
        if (this.monitoringRepository.count() == 0) {
            log.info("No active monitors found.");
        } else {
            if (this.availabilityRepository.count() == 0) {
                log.info("No timeslots are currently available, skipping.");
            } else {
                this.monitoringRepository.findAll().forEach(monitor -> {
                    log.info("Checking monitor for user {}...", monitor.getId());
                    val matchingTimeSlots = new ArrayList<TimeSlotAvailability>();
                    this.availabilityRepository.findAll().forEach(availability -> {
                        if (isInRange(availability.getDate(), monitor.getStartDate(), monitor.getEndDate())) {
                            log.info("Found available timeslot for user {} on {} at {}.", monitor.getId(), availability.getDate(), availability.getTime());
                            matchingTimeSlots.add(availability);
                        }
                    });
                    if (matchingTimeSlots.isEmpty()) {
                        log.info("No matching timeslots found for user {}.", monitor.getId());
                    } else {
                        this.monitoringRepository.deleteById(monitor.getId());
                        val message = new StringBuilder();
                        message.append("Time slots found for the requested dates:\n\n");
                        matchingTimeSlots.forEach(slot -> message.append(String.format("%s --> %s\n", slot.getDate(), slot.getTime())));
                        message.append("\n");
                        message.append("Monitoring has been deactivated automatically.\n");
                        message.append("Make reservation: https://kolejkagdansk.ajhmedia.pl/branch/5");
                        this.publisher.publishEvent(new EventTelegramOutboundMessage(this, monitor.getId(), message.toString()));
                    }
                });
            }
        }
    }

    private boolean isInRange(final LocalDate date, final LocalDate from, final LocalDate to) {
        return (date.isAfter(from) || date.isEqual(from)) && (date.isBefore(to) || date.isEqual(to));
    }
}
