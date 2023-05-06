package com.github.pannull.pomeraniabot.telegram.flows;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.pannull.pomeraniabot.timeslots.monitoring.TimeSlotMonitoring;
import com.github.pannull.pomeraniabot.timeslots.monitoring.TimeSlotMonitoringRepository;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TelegramFlowNewMonitor extends TelegramFlow {

    private final StateMachineConfig<State,Trigger> flowConfig = new StateMachineConfig<>();
    private final StateMachine<State,Trigger> flow = new StateMachine<>(State.NOT_STARTED, flowConfig);
    private final TimeSlotMonitoringRepository monitoringRepository;
    private final TimeSlotMonitoring chatMonitor;
    private String input;

    private enum State { NOT_STARTED, INITIALIZATION, WAITING_FOR_START_DATE, WAITING_FOR_END_DATE, COMPLETED }
    private enum Trigger { START, INITIALIZED, START_DATE_ENTERED, START_DATE_VERIFIED, END_DATE_ENTERED, END_DATE_VERIFIED, ALREADY_EXISTS, DATE_FORMAT_INVALID }

    public TelegramFlowNewMonitor(long chatId, ApplicationEventPublisher publisher, TimeSlotMonitoringRepository monitoringRepository) {
        super(chatId, publisher);
        this.monitoringRepository = monitoringRepository;
        this.chatMonitor = new TimeSlotMonitoring();
        this.flowConfig.configure(State.NOT_STARTED).permit(Trigger.START, State.INITIALIZATION);
        this.flowConfig.configure(State.INITIALIZATION)
                .onEntry(() -> log.info("New monitor flow started for chatId: {}", this.chatId))
                .onEntry(() -> {
                    if (this.monitoringRepository.findById(this.chatId).isPresent()) {
                        this.sendMessage("You already have a monitoring configured.");
                        this.flow.fire(Trigger.ALREADY_EXISTS);
                    } else {
                        this.chatMonitor.setId(this.chatId);
                        this.flow.fire(Trigger.INITIALIZED);
                    }})
                .permit(Trigger.INITIALIZED, State.WAITING_FOR_START_DATE)
                .permit(Trigger.ALREADY_EXISTS, State.COMPLETED);
        this.flowConfig.configure(State.WAITING_FOR_START_DATE)
                .onEntryFrom(Trigger.INITIALIZED, () -> this.sendMessage("Enter search start date (use the format DD/MM/YYYY):"))
                .onEntryFrom(Trigger.DATE_FORMAT_INVALID, () -> this.sendMessage("Invalid date format. Please try again."))
                .onExit(() -> log.info("Start date entered: {}", input))
                .permitInternal(Trigger.START_DATE_ENTERED, () -> {
                    try {
                        var date = LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        this.chatMonitor.setStartDate(date);
                        this.flow.fire(Trigger.START_DATE_VERIFIED);
                    } catch (Exception e) {
                        log.error("Error parsing date", e);
                        this.flow.fire(Trigger.DATE_FORMAT_INVALID);
                    }
                })
                .permit(Trigger.START_DATE_VERIFIED, State.WAITING_FOR_END_DATE)
                .permitReentry(Trigger.DATE_FORMAT_INVALID);
        this.flowConfig.configure(State.WAITING_FOR_END_DATE)
                .onEntryFrom(Trigger.START_DATE_VERIFIED, () -> this.sendMessage("Enter search end date (use the format DD/MM/YYYY):"))
                .onEntryFrom(Trigger.DATE_FORMAT_INVALID, () -> this.sendMessage("Invalid date format. Please try again."))
                .onExit(() -> log.info("End date entered: {}", input))
                .permitInternal(Trigger.END_DATE_ENTERED, () -> {
                    try {
                        var date = LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        this.chatMonitor.setEndDate(date);
                        this.flow.fire(Trigger.END_DATE_VERIFIED);
                    } catch (Exception e) {
                        log.error("Error parsing date", e);
                        this.flow.fire(Trigger.DATE_FORMAT_INVALID);
                    }
                })
                .permit(Trigger.END_DATE_VERIFIED, State.COMPLETED)
                .permitReentry(Trigger.DATE_FORMAT_INVALID);
        this.flowConfig.configure(State.COMPLETED)
                .onEntryFrom(Trigger.ALREADY_EXISTS, () -> log.info("Monitor already exists for chatId: {}", this.chatId))
                .onEntryFrom(Trigger.END_DATE_VERIFIED, () -> {
                    this.monitoringRepository.save(this.chatMonitor);
                    this.sendMessage("Your monitoring has been saved.");
                    log.info("New monitor flow completed for chatId: {}", this.chatId);
                })
                .onEntry(this::completeFlow);
    }

    @Override
    public void doNext(String input) {
        log.info("Input received, checking the flow state: {}", input);
        this.input = input;
        switch (flow.getState()) {
            case NOT_STARTED -> flow.fire(Trigger.START);
            case WAITING_FOR_START_DATE -> flow.fire(Trigger.START_DATE_ENTERED);
            case WAITING_FOR_END_DATE -> flow.fire(Trigger.END_DATE_ENTERED);
            default -> log.warn("Unexpected flow state on input received, ignoring: {}", flow.getState());
        }
    }

    @Override
    public boolean isCompleted() {
        return flow.isInState(State.COMPLETED);
    }
}
