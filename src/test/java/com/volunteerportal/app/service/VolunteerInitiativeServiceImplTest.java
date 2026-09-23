package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.model.VolunteerInitiativeAnswer;
import com.volunteerportal.app.repository.EventRepository;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VolunteerInitiativeServiceImplTest {

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private InitiativeQuestionRepository initiativeQuestionRepository;

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @Mock
    private VolunteerInitiativeAnswerRepository volunteerInitiativeAnswerRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private VolunteerInitiativeServiceImpl service;

    private User user;
    private Initiative initiative;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        initiative = new Initiative();
        initiative.setId(5L);
        initiative.setEnabled(true);
    }

    @Test
    void join_disabledInitiative_throws() {
        initiative.setEnabled(false);
        given(initiativeRepository.findById(5L)).willReturn(Optional.of(initiative));

        assertThatThrownBy(() -> service.join(5L, user, new LinkedMultiValueMap<>()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void join_alreadyRequested_throws() {
        given(initiativeRepository.findById(5L)).willReturn(Optional.of(initiative));
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(1L, 5L))
                .willReturn(Optional.of(new VolunteerInitiative()));

        assertThatThrownBy(() -> service.join(5L, user, new LinkedMultiValueMap<>()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void join_populatesAnswersPerQuestionTypeAndSetsAnswerCount() {
        given(initiativeRepository.findById(5L)).willReturn(Optional.of(initiative));
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(1L, 5L)).willReturn(Optional.empty());
        given(volunteerInitiativeRepository.save(any(VolunteerInitiative.class)))
                .willAnswer(inv -> inv.getArgument(0));

        InitiativeQuestion trueFalse = question(1L, 1, null);
        InitiativeQuestion singleChoice = question(2L, 2, "Red,Green,Blue");
        InitiativeQuestion multiChoice = question(3L, 3, "Cats,Dogs,Birds");
        InitiativeQuestion freeText = question(4L, 4, null);
        InitiativeQuestion unanswered = question(5L, 4, null);

        given(initiativeQuestionRepository.findByInitiativeId(5L))
                .willReturn(List.of(trueFalse, singleChoice, multiChoice, freeText, unanswered));

        MultiValueMap<String, String> answers = new LinkedMultiValueMap<>();
        answers.add("answer_1", "1");
        answers.add("answer_2", "2");
        answers.add("answer_3", "1");
        answers.add("answer_3", "3");
        answers.add("answer_4", "Free text response");

        ArgumentCaptor<VolunteerInitiativeAnswer> captor = ArgumentCaptor.forClass(VolunteerInitiativeAnswer.class);

        VolunteerInitiative result = service.join(5L, user, answers);

        assertThat(result.getAnswerCount()).isEqualTo(4);
        verify(volunteerInitiativeAnswerRepository, times(4)).save(captor.capture());

        List<VolunteerInitiativeAnswer> saved = captor.getAllValues();
        assertThat(saved).extracting(a -> a.getInitiativeQuestion().getId())
                .containsExactly(1L, 2L, 3L, 4L);

        VolunteerInitiativeAnswer trueFalseAnswer = saved.get(0);
        assertThat(trueFalseAnswer.getAnswerChoiceNumber()).isEqualTo(1);
        assertThat(trueFalseAnswer.getAnswerText()).isEqualTo("Yes");

        VolunteerInitiativeAnswer singleChoiceAnswer = saved.get(1);
        assertThat(singleChoiceAnswer.getAnswerChoiceNumber()).isEqualTo(2);
        assertThat(singleChoiceAnswer.getAnswerText()).isEqualTo("Green");

        VolunteerInitiativeAnswer multiChoiceAnswer = saved.get(2);
        assertThat(multiChoiceAnswer.getAnswerChoiceNumber()).isEqualTo(1);
        assertThat(multiChoiceAnswer.getAnswerText()).isEqualTo("Cats, Birds");

        VolunteerInitiativeAnswer freeTextAnswer = saved.get(3);
        assertThat(freeTextAnswer.getAnswerText()).isEqualTo("Free text response");
    }

    @Test
    void withdraw_pendingRequest_deletesAnswersThenMembership() {
        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setId(7L);
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(1L, 5L))
                .willReturn(Optional.of(membership));

        service.withdraw(5L, 1L);

        InOrder order = inOrder(volunteerInitiativeAnswerRepository, volunteerInitiativeRepository);
        order.verify(volunteerInitiativeAnswerRepository).deleteByVolunteerInitiativeId(7L);
        order.verify(volunteerInitiativeRepository).delete(membership);
    }

    @Test
    void withdraw_alreadyReviewed_throws() {
        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setId(8L);
        membership.setResponseJoinDttm(LocalDateTime.now());
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(1L, 5L))
                .willReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.withdraw(5L, 1L))
                .isInstanceOf(IllegalStateException.class);

        verify(volunteerInitiativeAnswerRepository, never()).deleteByVolunteerInitiativeId(anyLong());
        verify(volunteerInitiativeRepository, never()).delete(any());
    }

    @Test
    void withdraw_noExistingRequest_throwsEntityNotFound() {
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(1L, 5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(5L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private InitiativeQuestion question(Long id, int typeId, String choicesText) {
        InitiativeQuestion question = new InitiativeQuestion();
        question.setId(id);
        question.setQuestionTypeId(typeId);
        question.setQuestionChoicesText(choicesText);
        return question;
    }

    @Test
    void join_notifiesSupervisorAndOfficeCoordinator() {
        user.setUsername("vol");
        User supervisor = userWithId(20L);
        User officeCoordinator = userWithId(30L);
        initiative.setName("Beach Cleanup");
        initiative.setSupervisor(supervisor);
        initiative.setOffice(officeCoordinatedBy(officeCoordinator));
        stubSuccessfulJoin();

        service.join(5L, user, new LinkedMultiValueMap<>());

        verify(notificationService).notify(eq(supervisor), contains("vol requested to join 'Beach Cleanup'"), eq("/coordinator/requests"));
        verify(notificationService).notify(eq(officeCoordinator), contains("Beach Cleanup"), eq("/coordinator/requests"));
    }

    @Test
    void join_supervisorIsAlsoOfficeCoordinator_isNotifiedOnce() {
        User manager = userWithId(20L);
        initiative.setSupervisor(manager);
        initiative.setOffice(officeCoordinatedBy(userWithId(20L)));
        stubSuccessfulJoin();

        service.join(5L, user, new LinkedMultiValueMap<>());

        verify(notificationService, times(1)).notify(any(User.class), anyString(), anyString());
    }

    @Test
    void join_noManagers_sendsNoNotification() {
        stubSuccessfulJoin();

        service.join(5L, user, new LinkedMultiValueMap<>());

        verify(notificationService, never()).notify(any(User.class), anyString(), anyString());
    }

    @Test
    void join_managerRequestingThemselves_isNotNotified() {
        initiative.setSupervisor(userWithId(1L)); // same id as the requesting user
        stubSuccessfulJoin();

        service.join(5L, user, new LinkedMultiValueMap<>());

        verify(notificationService, never()).notify(any(User.class), anyString(), anyString());
    }

    private void stubSuccessfulJoin() {
        given(initiativeRepository.findById(5L)).willReturn(Optional.of(initiative));
        given(volunteerInitiativeRepository.findByUserIdAndInitiativeId(1L, 5L)).willReturn(Optional.empty());
        given(volunteerInitiativeRepository.save(any(VolunteerInitiative.class))).willAnswer(inv -> inv.getArgument(0));
        given(initiativeQuestionRepository.findByInitiativeId(5L)).willReturn(List.of());
    }

    private User userWithId(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Office officeCoordinatedBy(User coordinator) {
        Office office = new Office();
        office.setUser(coordinator);
        return office;
    }
}
