package com.volunteerportal.app.controller;

import org.springframework.validation.BindingResult;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

/** Attendance form rule shared by the admin and coordinator attendance pages. */
final class AttendanceRules {

    private AttendanceRules() {
    }

    /** Attendance can only be recorded for volunteers whose request to join this initiative was approved. */
    static void rejectIfNotApprovedMember(AttendForm form, Long initiativeId, BindingResult bindingResult,
            VolunteerInitiativeRepository volunteerInitiativeRepository) {
        if (form.getVolunteerInitiativeId() == null) {
            return; // @NotNull reports this one
        }
        boolean approvedMember = volunteerInitiativeRepository.findById(form.getVolunteerInitiativeId())
                .filter(vi -> Boolean.TRUE.equals(vi.getEnabled()))
                .filter(vi -> vi.getInitiative() != null && initiativeId.equals(vi.getInitiative().getId()))
                .isPresent();
        if (!approvedMember) {
            bindingResult.rejectValue("volunteerInitiativeId", "error.attend.notApprovedMember",
                    "Only approved members of this initiative can attend its events");
        }
    }
}
