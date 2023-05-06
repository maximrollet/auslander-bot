package com.github.pannull.pomeraniabot.telegram.flows;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.pannull.pomeraniabot.timeslots.monitoring.TimeSlotMonitoringRepository;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TelegramFlowDeleteMonitor extends TelegramFlow {

    private final StateMachineConfig<State,Trigger> flowConfig = new StateMachineConfig<>();
    private final StateMachine<State,Trigger> flow = new StateMachine<>(State.NOT_STARTED, flowConfig);
    private final TimeSlotMonitoringRepository monitoringsRepository;

    private enum State { NOT_STARTED, PROCESSING, COMPLETED }
    private enum Trigger { START, MONITORING_REMOVED, MONITORING_NOT_FOUND }

    public TelegramFlowDeleteMonitor(final long chatId, final ApplicationEventPublisher publisher, final TimeSlotMonitoringRepository repository) {
        super(chatId, publisher);
        this.monitoringsRepository = repository;
        this.flowConfig.configure(State.NOT_STARTED).permit(Trigger.START, State.PROCESSING);
        this.flowConfig.configure(State.PROCESSING)
                .onEntry(() -> log.info("Removing monitoring for chatId: " + this.chatId + "."))
                .onEntry(() -> {
                    if (this.monitoringsRepository.findById(this.chatId).isPresent()) {
                        this.monitoringsRepository.deleteById(this.chatId);
                        this.flow.fire(Trigger.MONITORING_REMOVED);
                    } else {
                        this.flow.fire(Trigger.MONITORING_NOT_FOUND);
                    }
                })
                .permit(Trigger.MONITORING_REMOVED, State.COMPLETED)
                .permit(Trigger.MONITORING_NOT_FOUND, State.COMPLETED);
        this.flowConfig.configure(State.COMPLETED)
                .onEntryFrom(Trigger.MONITORING_REMOVED, () -> this.sendMessage("Monitoring removed."))
                .onEntryFrom(Trigger.MONITORING_NOT_FOUND, () -> this.sendMessage("You don't have an active monitoring."))
                .onEntry(this::completeFlow);
    }

    @Override
    public void doNext(String input) {
        if (this.flow.getState() == State.NOT_STARTED) {
            this.flow.fire(Trigger.START);
        } else {
            log.warn("Flow is already completed.");
        }
    }

    @Override
    public boolean isCompleted() {
        return this.flow.getState() == State.COMPLETED;
    }
}
