package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InitiativeExportServiceImplTest {

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private VolunteerInitiativeRepository volunteerInitiativeRepository;

    @InjectMocks
    private InitiativeExportServiceImpl initiativeExportService;

    @Test
    void exportVolunteersCsv_missingInitiative_throwsEntityNotFound() {
        given(initiativeRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> initiativeExportService.exportVolunteersCsv(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void exportVolunteersCsv_rendersHeaderAndOneRowPerStatus() {
        given(initiativeRepository.existsById(1L)).willReturn(true);
        given(volunteerInitiativeRepository.findByInitiativeId(1L)).willReturn(List.of(
                request("approved_vol", "approved@example.com", true, 3),
                request("pending_vol", "pending@example.com", null, 0),
                request("rejected_vol", "rejected@example.com", false, 2)));

        String csv = initiativeExportService.exportVolunteersCsv(1L);

        List<String> lines = csv.lines().toList();
        assertThat(lines.get(0)).isEqualTo("Username,Email,Status,Requested At,Responded At,Answers Given");
        assertThat(lines.get(1)).startsWith("approved_vol,approved@example.com,Approved,");
        assertThat(lines.get(1)).endsWith(",3");
        assertThat(lines.get(2)).startsWith("pending_vol,pending@example.com,Pending,");
        assertThat(lines.get(2)).endsWith(",0");
        assertThat(lines.get(3)).startsWith("rejected_vol,rejected@example.com,Rejected,");
        assertThat(lines.get(3)).endsWith(",2");
    }

    @Test
    void exportVolunteersCsv_usernameContainingComma_isQuotedAndEscaped() {
        given(initiativeRepository.existsById(1L)).willReturn(true);
        given(volunteerInitiativeRepository.findByInitiativeId(1L)).willReturn(List.of(
                request("last, first", "quoted@example.com", true, 1)));

        String csv = initiativeExportService.exportVolunteersCsv(1L);

        assertThat(csv).contains("\"last, first\",quoted@example.com,Approved");
    }

    @Test
    void exportVolunteersCsv_formulaLikeValues_arePrefixedWithApostrophe() {
        given(initiativeRepository.existsById(1L)).willReturn(true);
        given(volunteerInitiativeRepository.findByInitiativeId(1L)).willReturn(List.of(
                request("=1+1", "plain@example.com", true, 1),
                request("+cmd", "@evil.example.com", true, 1),
                request("-2", "tab@example.com", true, 1),
                request("=HYPERLINK(\"http://evil\",\"x\")", "link@example.com", true, 1)));

        String csv = initiativeExportService.exportVolunteersCsv(1L);

        assertThat(csv).contains("\r\n'=1+1,plain@example.com,Approved");
        assertThat(csv).contains("\r\n'+cmd,'@evil.example.com,Approved");
        assertThat(csv).contains("\r\n'-2,tab@example.com,Approved");
        assertThat(csv).contains("\r\n\"'=HYPERLINK(\"\"http://evil\"\",\"\"x\"\")\",link@example.com,Approved");
    }

    @Test
    void exportVolunteersCsv_valueContainingCarriageReturn_isQuoted() {
        given(initiativeRepository.existsById(1L)).willReturn(true);
        given(volunteerInitiativeRepository.findByInitiativeId(1L)).willReturn(List.of(
                request("line\rbreak", "cr@example.com", true, 1)));

        String csv = initiativeExportService.exportVolunteersCsv(1L);

        assertThat(csv).contains("\r\n\"line\rbreak\",cr@example.com,Approved");
    }

    private VolunteerInitiative request(String username, String email, Boolean enabled, int answerCount) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        VolunteerInitiative request = new VolunteerInitiative();
        request.setUser(user);
        request.setEnabled(enabled);
        request.setAnswerCount(answerCount);
        request.setRequestJoinDttm(LocalDateTime.of(2026, 1, 1, 9, 0));
        if (enabled != null) {
            request.setResponseJoinDttm(LocalDateTime.of(2026, 1, 2, 9, 0));
        }
        return request;
    }
}
