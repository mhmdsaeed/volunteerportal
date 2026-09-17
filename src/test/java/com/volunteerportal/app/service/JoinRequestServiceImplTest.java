package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private JoinRequestServiceImpl joinRequestService;

    @Test
    void approve_setsResponseAndEnabledAndNotifiesVolunteer() {
        User volunteer = new User();
        volunteer.setUsername("vol");

        Initiative initiative = new Initiative();
        initiative.setId(10L);
        initiative.setName("Beach Cleanup");

        VolunteerInitiative request = new VolunteerInitiative();
        request.setId(1L);
        request.setUser(volunteer);
        request.setInitiative(initiative);

        given(volunteerInitiativeRepository.findById(1L)).willReturn(Optional.of(request));
        given(volunteerInitiativeRepository.save(request)).willReturn(request);

        VolunteerInitiative result = joinRequestService.approve(1L);

        assertThat(result.getResponseJoinDttm()).isNotNull();
        assertThat(result.getEnabled()).isTrue();
        verify(notificationService).notify(eq(volunteer), contains("Beach Cleanup"), eq("/initiatives/10"));
    }

    @Test
    void reject_setsResponseAndDisabledAndNotifiesVolunteer() {
        User volunteer = new User();
        volunteer.setUsername("vol2");

        Initiative initiative = new Initiative();
        initiative.setId(20L);
        initiative.setName("Food Drive");

        VolunteerInitiative request = new VolunteerInitiative();
        request.setId(2L);
        request.setUser(volunteer);
        request.setInitiative(initiative);

        given(volunteerInitiativeRepository.findById(2L)).willReturn(Optional.of(request));
        given(volunteerInitiativeRepository.save(request)).willReturn(request);

        VolunteerInitiative result = joinRequestService.reject(2L);

        assertThat(result.getResponseJoinDttm()).isNotNull();
        assertThat(result.getEnabled()).isFalse();
        verify(notificationService).notify(eq(volunteer), contains("Food Drive"), eq("/initiatives/20"));
    }

    @Test
    void findById_notFound_throwsEntityNotFound() {
        given(volunteerInitiativeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> joinRequestService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findManagedInitiatives_nullSupervisorId_returnsAllInitiatives() {
        Initiative initiative = new Initiative();
        initiative.setId(1L);
        given(initiativeRepository.findAll()).willReturn(List.of(initiative));

        List<Initiative> result = joinRequestService.findManagedInitiatives(null);

        assertThat(result).containsExactly(initiative);
        verify(initiativeRepository, never()).findBySupervisorId(anyLong());
    }

    @Test
    void findManagedInitiatives_withSupervisorId_returnsOnlyThatSupervisorsInitiatives() {
        Initiative initiative = new Initiative();
        initiative.setId(2L);
        given(initiativeRepository.findBySupervisorId(7L)).willReturn(List.of(initiative));

        List<Initiative> result = joinRequestService.findManagedInitiatives(7L);

        assertThat(result).containsExactly(initiative);
        verify(initiativeRepository, never()).findAll();
    }

    @Test
    void findRequestsForInitiative_delegatesToVolunteerInitiativeRepository() {
        VolunteerInitiative request = new VolunteerInitiative();
        request.setId(5L);
        given(volunteerInitiativeRepository.findByInitiativeId(3L)).willReturn(List.of(request));

        List<VolunteerInitiative> result = joinRequestService.findRequestsForInitiative(3L);

        assertThat(result).containsExactly(request);
    }
}
