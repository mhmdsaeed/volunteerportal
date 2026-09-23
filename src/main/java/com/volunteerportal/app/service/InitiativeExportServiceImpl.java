package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;

import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

@Service
public class InitiativeExportServiceImpl implements InitiativeExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] HEADER = { "Username", "Email", "Status", "Requested At", "Responded At", "Answers Given" };

    private final InitiativeRepository initiativeRepository;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;

    public InitiativeExportServiceImpl(InitiativeRepository initiativeRepository,
            VolunteerInitiativeRepository volunteerInitiativeRepository) {
        this.initiativeRepository = initiativeRepository;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
    }

    @Override
    public String exportVolunteersCsv(Long initiativeId) {
        if (!initiativeRepository.existsById(initiativeId)) {
            throw new EntityNotFoundException("Initiative not found: " + initiativeId);
        }

        List<VolunteerInitiative> requests = volunteerInitiativeRepository.findByInitiativeId(initiativeId);

        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADER);
        for (VolunteerInitiative request : requests) {
            appendRow(csv, toRow(request));
        }
        return csv.toString();
    }

    private String[] toRow(VolunteerInitiative request) {
        User user = request.getUser();
        return new String[] {
                user != null ? user.getUsername() : "",
                user != null ? user.getEmail() : "",
                status(request.getEnabled()),
                formatDate(request.getRequestJoinDttm()),
                formatDate(request.getResponseJoinDttm()),
                request.getAnswerCount() != null ? request.getAnswerCount().toString() : "0"
        };
    }

    private String status(Boolean enabled) {
        if (enabled == null) {
            return "Pending";
        }
        return enabled ? "Approved" : "Rejected";
    }

    private String formatDate(LocalDateTime dttm) {
        return dttm != null ? dttm.format(DATE_FORMAT) : "";
    }

    private void appendRow(StringBuilder csv, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(fields[i]));
        }
        csv.append("\r\n");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        // Neutralise spreadsheet formula injection (e.g. a username of "=HYPERLINK(...)")
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
