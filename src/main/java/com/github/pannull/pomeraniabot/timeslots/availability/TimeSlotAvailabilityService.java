package com.github.pannull.pomeraniabot.timeslots.availability;

import com.github.pannull.pomeraniabot.timeslots.availability.models.PomorskieAvailabilityResponseDates;
import com.github.pannull.pomeraniabot.timeslots.availability.models.PomorskieAvailabilityResponseTimes;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Slf4j
@Service
public class TimeSlotAvailabilityService {

    private final TimeSlotAvailabilityRepository availabilityRepository;
    private final String dateAvailabilityEndpoint;
    private final String timeAvailabilityEndpoint;
    private final RestTemplate restTemplate;

    @Autowired
    public TimeSlotAvailabilityService(@Value("${monitor.timeslot-dates-availability-endpoint}") String dateEndpoint,
                                       @Value("${monitor.timeslot-times-availability-endpoint}") String timeEndpoint,
                                       @Autowired TimeSlotAvailabilityRepository availabilityRepository) {

        this.restTemplate = new RestTemplate();
        this.dateAvailabilityEndpoint = dateEndpoint;
        this.timeAvailabilityEndpoint = timeEndpoint;
        this.availabilityRepository = availabilityRepository;
    }

    @Scheduled(fixedRate = 1000 * 60 * 4)
    public void doScanning() {
        log.info("Scanning for available timeslots...");
        val availableDates = restTemplate.getForObject(dateAvailabilityEndpoint, PomorskieAvailabilityResponseDates.class);
        if (availableDates != null && !availableDates.getAvailableDates().isEmpty()) {
            val timeSlotsFound = new ArrayList<TimeSlotAvailability>();
            availableDates.getAvailableDates().forEach(date -> {
                val availableTimes = restTemplate.getForObject(timeAvailabilityEndpoint + "/" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), PomorskieAvailabilityResponseTimes.class);
                availableTimes.getTimeSlots().forEach(time -> {
                    val timeSlot = new TimeSlotAvailability();
                    timeSlot.setDate(date);
                    timeSlot.setTime(LocalTime.parse(time.getTime()));
                    timeSlotsFound.add(timeSlot);
                });
            });
            log.info("Found {} available timeslots.", timeSlotsFound.size());
            this.availabilityRepository.deleteAll();
            this.availabilityRepository.saveAll(timeSlotsFound);
        } else {
            log.info("No available dates found.");
            this.availabilityRepository.deleteAll();
        }
    }
}