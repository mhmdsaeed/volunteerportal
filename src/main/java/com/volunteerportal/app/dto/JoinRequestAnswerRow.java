package com.volunteerportal.app.dto;

/**
 * One of an initiative's questions with a volunteer's answer to it, for reviewing their join request.
 * typeId is the question's type (1 yes/no, 2 one choice, 3 several choices, 4 text); answer is null when
 * the volunteer left the question blank. For a yes/no answer, choiceNumber is 1 (yes) or 2 (no), so the
 * page can show it in the reviewer's language.
 */
public record JoinRequestAnswerRow(
        String question,
        Integer typeId,
        Integer choiceNumber,
        String answer) {
}
