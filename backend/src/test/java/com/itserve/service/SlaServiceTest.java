package com.itserve.service;

import com.itserve.entity.Priority;
import com.itserve.entity.SlaRule;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.SlaRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private SlaRuleRepository slaRuleRepository;

    private SlaService slaService;

    @BeforeEach
    void setUp() {
        slaService = new SlaService(slaRuleRepository);
    }

    @Test
    void deadlineIsCreatedAtPlusRuleHours() {
        when(slaRuleRepository.findByPriority(Priority.HIGH)).thenReturn(Optional.of(new SlaRule(Priority.HIGH, 4)));
        Instant createdAt = Instant.parse("2026-09-27T10:00:00Z");

        assertThat(slaService.computeDeadline(Priority.HIGH, createdAt))
                .isEqualTo(Instant.parse("2026-09-27T14:00:00Z"));
    }

    @Test
    void criticalTicketsGetTheShortestDeadline() {
        when(slaRuleRepository.findByPriority(Priority.CRITICAL)).thenReturn(Optional.of(new SlaRule(Priority.CRITICAL, 2)));
        Instant createdAt = Instant.parse("2026-09-27T10:00:00Z");

        assertThat(slaService.computeDeadline(Priority.CRITICAL, createdAt))
                .isEqualTo(createdAt.plusSeconds(2 * 3600));
    }

    @Test
    void missingRuleFailsLoudly() {
        when(slaRuleRepository.findByPriority(Priority.LOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slaService.computeDeadline(Priority.LOW, Instant.now()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
