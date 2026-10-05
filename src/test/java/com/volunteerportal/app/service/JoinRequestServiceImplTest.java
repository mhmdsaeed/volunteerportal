package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.dto.JoinRequestAnswerRow;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.model.VolunteerInitiativeAnswer;
import com.volunteerportal.app.repository.InitiativeQuestionRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeAnswerRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private InitiativeQuestionRepository initiativeQuestionRepository;

    @Mock
    private VolunteerInitiativeAnswerRepository answerRepository;

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
        verify(notificationService).notify(eq(volunteer), eq("notification.joinApproved"), eq("/initiatives/10"), eq("Beach Cleanup"));
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
        verify(notificationService).notify(eq(volunteer), eq("notification.joinRejected"), eq("/initiatives/20"), eq("Food Drive"));
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
        verify(initiativeRepository, never()).findManagedBy(anyLong());
    }

    @Test
    void findManagedInitiatives_withManagerId_returnsInitiativesTheySuperviseOrCoordinate() {
        Initiative initiative = new Initiative();
        initiative.setId(2L);
        given(initiativeRepository.findManagedBy(7L)).willReturn(List.of(initiative));

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

    @Test
    void canManage_supervisor_isTrue() {
        assertThat(joinRequestService.canManage(initiativeManagedBy(7L, null), 7L)).isTrue();
    }

    @Test
    void canManage_officeCoordinator_isTrue() {
        assertThat(joinRequestService.canManage(initiativeManagedBy(null, 8L), 8L)).isTrue();
    }

    @Test
    void canManage_unrelatedUser_isFalse() {
        assertThat(joinRequestService.canManage(initiativeManagedBy(7L, 8L), 9L)).isFalse();
    }

    @Test
    void canManage_initiativeWithoutSupervisorOrOffice_isFalse() {
        assertThat(joinRequestService.canManage(new Initiative(), 7L)).isFalse();
    }

    @Test
    void findPendingRequests_nullManager_returnsEveryPendingRequest() {
        VolunteerInitiative pending = new VolunteerInitiative();
        given(volunteerInitiativeRepository.findByResponseJoinDttmIsNullOrderByRequestJoinDttmAsc()).willReturn(List.of(pending));

        assertThat(joinRequestService.findPendingRequests(null)).containsExactly(pending);
        verify(volunteerInitiativeRepository, never()).findPendingManagedBy(anyLong());
    }

    @Test
    void findPendingRequests_withManager_returnsOnlyTheirPendingRequests() {
        VolunteerInitiative pending = new VolunteerInitiative();
        given(volunteerInitiativeRepository.findPendingManagedBy(7L)).willReturn(List.of(pending));

        assertThat(joinRequestService.findPendingRequests(7L)).containsExactly(pending);
    }

    @Test
    void countPendingRequests_scopesLikeFindPendingRequests() {
        given(volunteerInitiativeRepository.countByResponseJoinDttmIsNull()).willReturn(5L);
        given(volunteerInitiativeRepository.countPendingManagedBy(7L)).willReturn(2L);

        assertThat(joinRequestService.countPendingRequests(null)).isEqualTo(5L);
        assertThat(joinRequestService.countPendingRequests(7L)).isEqualTo(2L);
    }

    @Test
    void findAnswers_listsEveryQuestionInOrder_withTheAnswerOrNone() {
        Initiative initiative = new Initiative();
        initiative.setId(5L);
        VolunteerInitiative request = new VolunteerInitiative();
        request.setId(77L);
        request.setInitiative(initiative);
        given(volunteerInitiativeRepository.findById(77L)).willReturn(Optional.of(request));
        InitiativeQuestion yesNo = question(11L, 1, "Are you over 18?");
        InitiativeQuestion text = question(12L, 4, "Tell us about yourself");
        InitiativeQuestion blank = question(13L, 4, "Anything else?");
        given(initiativeQuestionRepository.findByInitiativeId(5L)).willReturn(List.of(blank, text, yesNo));
        given(answerRepository.findByVolunteerInitiativeId(77L)).willReturn(List.of(
                answer(text, null, "I run a food bank"), answer(yesNo, 2, "No")));

        assertThat(joinRequestService.findAnswers(77L)).containsExactly(
                new JoinRequestAnswerRow("Are you over 18?", 1, 2, "No"),
                new JoinRequestAnswerRow("Tell us about yourself", 4, null, "I run a food bank"),
                new JoinRequestAnswerRow("Anything else?", 4, null, null));
    }

    private static InitiativeQuestion question(Long id, int typeId, String text) {
        InitiativeQuestion question = new InitiativeQuestion();
        question.setId(id);
        question.setQuestionTypeId(typeId);
        question.setQuestionText(text);
        return question;
    }

    private static VolunteerInitiativeAnswer answer(InitiativeQuestion question, Integer choice, String text) {
        VolunteerInitiativeAnswer answer = new VolunteerInitiativeAnswer();
        answer.setInitiativeQuestion(question);
        answer.setAnswerChoiceNumber(choice);
        answer.setAnswerText(text);
        return answer;
    }

    @Test
    void approve_alreadyDecided_throwsAndDoesNotNotifyAgain() {
        VolunteerInitiative decided = new VolunteerInitiative();
        decided.setId(3L);
        decided.setResponseJoinDttm(LocalDateTime.now());
        decided.setEnabled(false);
        given(volunteerInitiativeRepository.findById(3L)).willReturn(Optional.of(decided));

        assertThatThrownBy(() -> joinRequestService.approve(3L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> joinRequestService.reject(3L)).isInstanceOf(IllegalStateException.class);

        assertThat(decided.getEnabled()).isFalse();
        verify(volunteerInitiativeRepository, never()).save(any());
        verify(notificationService, never()).notify(any(), anyString(), anyString(), any(String[].class));
    }

    private Initiative initiativeManagedBy(Long supervisorId, Long officeCoordinatorId) {
        Initiative initiative = new Initiative();
        if (supervisorId != null) {
            User supervisor = new User();
            supervisor.setId(supervisorId);
            initiative.setSupervisor(supervisor);
        }
        if (officeCoordinatorId != null) {
            User coordinator = new User();
            coordinator.setId(officeCoordinatorId);
            Office office = new Office();
            office.setUser(coordinator);
            initiative.setOffice(office);
        }
        return initiative;
    }
}
