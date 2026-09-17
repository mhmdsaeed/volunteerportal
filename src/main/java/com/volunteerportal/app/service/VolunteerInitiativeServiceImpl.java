package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.InitiativeQuestion;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.model.VolunteerInitiativeAnswer;
import com.volunteerportal.app.repository.InitiativeQuestionRepository;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeAnswerRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

@Service
public class VolunteerInitiativeServiceImpl implements VolunteerInitiativeService {

    private final InitiativeRepository initiativeRepository;
    private final InitiativeQuestionRepository initiativeQuestionRepository;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;
    private final VolunteerInitiativeAnswerRepository volunteerInitiativeAnswerRepository;

    public VolunteerInitiativeServiceImpl(InitiativeRepository initiativeRepository,
            InitiativeQuestionRepository initiativeQuestionRepository,
            VolunteerInitiativeRepository volunteerInitiativeRepository,
            VolunteerInitiativeAnswerRepository volunteerInitiativeAnswerRepository) {
        this.initiativeRepository = initiativeRepository;
        this.initiativeQuestionRepository = initiativeQuestionRepository;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
        this.volunteerInitiativeAnswerRepository = volunteerInitiativeAnswerRepository;
    }

    @Override
    public List<Initiative> findAvailableInitiatives() {
        return initiativeRepository.findByEnabledTrue();
    }

    @Override
    public Initiative findInitiativeDetail(Long initiativeId) {
        return initiativeRepository.findById(initiativeId)
                .orElseThrow(() -> new EntityNotFoundException("Initiative not found: " + initiativeId));
    }

    @Override
    public List<InitiativeQuestion> findQuestions(Long initiativeId) {
        return initiativeQuestionRepository.findByInitiativeId(initiativeId);
    }

    @Override
    public Optional<VolunteerInitiative> findMembership(Long userId, Long initiativeId) {
        return volunteerInitiativeRepository.findByUserIdAndInitiativeId(userId, initiativeId);
    }

    @Override
    public Map<Long, VolunteerInitiative> findMembershipsForUser(Long userId) {
        return volunteerInitiativeRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(vi -> vi.getInitiative().getId(), vi -> vi));
    }

    @Override
    @Transactional
    public VolunteerInitiative join(Long initiativeId, User user, MultiValueMap<String, String> answers) {
        Initiative initiative = findInitiativeDetail(initiativeId);
        if (!Boolean.TRUE.equals(initiative.getEnabled())) {
            throw new IllegalStateException("Initiative is not open for enrollment: " + initiativeId);
        }
        if (volunteerInitiativeRepository.findByUserIdAndInitiativeId(user.getId(), initiativeId).isPresent()) {
            throw new IllegalStateException("Already requested to join this initiative");
        }

        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setUser(user);
        membership.setInitiative(initiative);
        membership.setRequestJoinDttm(LocalDateTime.now());
        membership = volunteerInitiativeRepository.save(membership);

        int answeredCount = saveAnswers(membership, initiativeQuestionRepository.findByInitiativeId(initiativeId),
                answers);

        membership.setAnswerCount(answeredCount);
        return volunteerInitiativeRepository.save(membership);
    }

    private int saveAnswers(VolunteerInitiative membership, List<InitiativeQuestion> questions,
            MultiValueMap<String, String> answers) {
        int answeredCount = 0;

        for (InitiativeQuestion question : questions) {
            List<String> rawValues = answers.get("answer_" + question.getId());
            List<String> values = rawValues == null ? List.of()
                    : rawValues.stream().filter(v -> v != null && !v.isBlank()).toList();
            if (values.isEmpty()) {
                continue;
            }

            VolunteerInitiativeAnswer answer = new VolunteerInitiativeAnswer();
            answer.setVolunteerInitiative(membership);
            answer.setInitiativeQuestion(question);
            answer.setAnswerDttm(LocalDateTime.now());
            populateAnswer(answer, question, values);

            volunteerInitiativeAnswerRepository.save(answer);
            answeredCount++;
        }

        return answeredCount;
    }

    private void populateAnswer(VolunteerInitiativeAnswer answer, InitiativeQuestion question, List<String> values) {
        int typeId = question.getQuestionTypeId() != null ? question.getQuestionTypeId() : 4;
        List<String> choices = splitChoices(question.getQuestionChoicesText());

        switch (typeId) {
            case 1 -> { // true_false
                boolean yes = "1".equals(values.get(0));
                answer.setAnswerChoiceNumber(yes ? 1 : 2);
                answer.setAnswerText(yes ? "Yes" : "No");
            }
            case 2 -> { // one_of_n
                int index = parseIndex(values.get(0));
                answer.setAnswerChoiceNumber(index);
                answer.setAnswerText(labelFor(choices, index));
            }
            case 3 -> { // multi_of_n
                List<Integer> indices = values.stream().map(this::parseIndex).filter(i -> i > 0).toList();
                answer.setAnswerChoiceNumber(indices.isEmpty() ? null : indices.get(0));
                answer.setAnswerText(indices.stream()
                        .map(i -> labelFor(choices, i))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")));
            }
            default -> answer.setAnswerText(values.get(0)); // free_text
        }
    }

    private List<String> splitChoices(String choicesText) {
        if (choicesText == null || choicesText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(choicesText.split(",")).map(String::trim).toList();
    }

    private String labelFor(List<String> choices, int index) {
        if (index < 1 || index > choices.size()) {
            return null;
        }
        return choices.get(index - 1);
    }

    private int parseIndex(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
