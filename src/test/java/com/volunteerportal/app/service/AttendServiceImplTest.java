package com.volunteerportal.app.service;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendServiceImplTest {

    @Mock
    private AttendRepository attendRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @InjectMocks
    private AttendServiceImpl attendService;

    @Test
    void create_approvedMemberOfTheEventsInitiative_saves() {
        stubEventOfInitiative(5L);
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(5L, true)));
        given(attendRepository.save(any(Attend.class))).willAnswer(inv -> inv.getArgument(0));

        Attend saved = attendService.create(9L, form(3L));

        assertThat(saved.getVolunteerInitiative().getId()).isEqualTo(3L);
    }

    @Test
    void create_pendingMember_isRefused() {
        stubEventOfInitiative(5L);
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(5L, null)));

        assertThatThrownBy(() -> attendService.create(9L, form(3L))).isInstanceOf(IllegalStateException.class);
        verify(attendRepository, never()).save(any());
    }

    @Test
    void create_memberOfAnotherInitiative_isRefused() {
        stubEventOfInitiative(5L);
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(member(6L, true)));

        assertThatThrownBy(() -> attendService.create(9L, form(3L))).isInstanceOf(IllegalStateException.class);
        verify(attendRepository, never()).save(any());
    }

    private void stubEventOfInitiative(Long initiativeId) {
        Event event = new Event();
        event.setId(9L);
        event.setInitiative(initiative(initiativeId));
        given(eventRepository.findById(9L)).willReturn(Optional.of(event));
    }

    private VolunteerInitiative member(Long initiativeId, Boolean approved) {
        VolunteerInitiative member = new VolunteerInitiative();
        member.setId(3L);
        member.setInitiative(initiative(initiativeId));
        member.setEnabled(approved);
        return member;
    }

    private Initiative initiative(Long id) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        return initiative;
    }

    private AttendForm form(Long volunteerInitiativeId) {
        AttendForm form = new AttendForm();
        form.setVolunteerInitiativeId(volunteerInitiativeId);
        form.setAttendInOut(1);
        return form;
    }
}
