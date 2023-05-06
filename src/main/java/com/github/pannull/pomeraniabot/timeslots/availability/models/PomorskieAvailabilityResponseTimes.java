package com.github.pannull.pomeraniabot.timeslots.availability.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;

@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class PomorskieAvailabilityResponseTimes {

    @JsonProperty("TIMES") ArrayList<TimeSlot> timeSlots;

    @Value
    @Builder
    @Jacksonized
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeSlot {
        @JsonProperty("time")               String time;
        @JsonProperty("slots")              int slotsAvailable;
        @JsonProperty("reservations_count") int reservationsCount;
        @JsonProperty("max_slots")          int maxSlots;
    }
}
