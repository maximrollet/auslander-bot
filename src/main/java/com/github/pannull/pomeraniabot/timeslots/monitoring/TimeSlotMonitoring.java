package com.github.pannull.pomeraniabot.timeslots.monitoring;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
public class TimeSlotMonitoring {

    @Id
    private long id;
    private LocalDate startDate;
    private LocalDate endDate;
}