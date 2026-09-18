package com.volunteerportal.app.service;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.InitiativeQuestionRepository;
import com.volunteerportal.app.repository.InitiativeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InitiativeDuplicateServiceImplTest {

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private InitiativeQuestionRepository initiativeQuestionRepository;

    @InjectMocks
    private InitiativeDuplicateServiceImpl initiativeDuplicateService;

    @Test
    void duplicate_missingInitiative_throwsEntityNotFound() {
        given(initiativeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> initiativeDuplicateService.duplicate(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void duplicate_copiesFieldsAndDisablesTheCopyByDefault() {
        Office office = new Office();
        office.setId(3L);
        User supervisor = new User();
        supervisor.setId(4L);

        Initiative source = new Initiative();
        source.setId(1L);
        source.setName("Beach Cleanup");
        source.setDescription("Clean the beach");
        source.setOffice(office);
        source.setSupervisor(supervisor);
        source.setEnabled(true);

        given(initiativeRepository.findById(1L)).willReturn(Optional.of(source));
        given(initiativeRepository.save(any(Initiative.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(initiativeQuestionRepository.findByInitiativeId(1L)).willReturn(List.of());

        Initiative result = initiativeDuplicateService.duplicate(1L);

        assertThat(result.getName()).isEqualTo("Beach Cleanup (Copy)");
        assertThat(result.getDescription()).isEqualTo("Clean the beach");
        assertThat(result.getOffice()).isSameAs(office);
        assertThat(result.getSupervisor()).isSameAs(supervisor);
        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getQuestionCount()).isZero();
    }

    @Test
    void duplicate_copiesEachQuestionAndSetsQuestionCount() {
        Initiative source = new Initiative();
        source.setId(1L);
        source.setName("Beach Cleanup");

        InitiativeQuestion q1 = new InitiativeQuestion();
        q1.setQuestionText("Can you swim?");
        q1.setQuestionTypeId(1);

        InitiativeQuestion q2 = new InitiativeQuestion();
        q2.setQuestionText("Shirt size?");
        q2.setQuestionTypeId(2);
        q2.setQuestionChoicesCount(3);
        q2.setQuestionChoicesText("S,M,L");

        given(initiativeRepository.findById(1L)).willReturn(Optional.of(source));
        given(initiativeRepository.save(any(Initiative.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(initiativeQuestionRepository.findByInitiativeId(1L)).willReturn(List.of(q1, q2));

        Initiative result = initiativeDuplicateService.duplicate(1L);

        assertThat(result.getQuestionCount()).isEqualTo(2);

        ArgumentCaptor<InitiativeQuestion> captor = ArgumentCaptor.forClass(InitiativeQuestion.class);
        verify(initiativeQuestionRepository, times(2)).save(captor.capture());

        List<InitiativeQuestion> savedQuestions = captor.getAllValues();
        assertThat(savedQuestions).extracting(InitiativeQuestion::getQuestionText)
                .containsExactly("Can you swim?", "Shirt size?");
        assertThat(savedQuestions).allMatch(q -> q.getInitiative() == result);
        assertThat(savedQuestions.get(1).getQuestionChoicesCount()).isEqualTo(3);
        assertThat(savedQuestions.get(1).getQuestionChoicesText()).isEqualTo("S,M,L");
    }

    @Test
    void duplicate_sourceWithNoOfficeOrSupervisor_copiesNulls() {
        Initiative source = new Initiative();
        source.setId(1L);
        source.setName("Food Drive");

        given(initiativeRepository.findById(1L)).willReturn(Optional.of(source));
        given(initiativeRepository.save(any(Initiative.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(initiativeQuestionRepository.findByInitiativeId(1L)).willReturn(List.of());

        Initiative result = initiativeDuplicateService.duplicate(1L);

        assertThat(result.getOffice()).isNull();
        assertThat(result.getSupervisor()).isNull();
    }
}
