package com.volunteerportal.app.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.model.Attend;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.AttendService;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;

/**
 * Lets an initiative's coordinators (its supervisor or its office's coordinator) record, fix and
 * remove attendance for the initiative's events; admins can use it for any initiative. Reuses the
 * admin attendance templates, pointed at these URLs.
 */
@Controller
@RequestMapping("/coordinator/initiatives/{initiativeId}/events/{eventId}/attendance")
public class CoordinatorAttendController {

    private static final String LIST_VIEW = "admin/initiatives/events/attendance/list";
    private static final String FORM_VIEW = "admin/initiatives/events/attendance/form";
    private static final String REDIRECT_TO_LIST = "redirect:/coordinator/initiatives/{initiativeId}/events/{eventId}/attendance";

    private final AttendService attendService;
    private final EventService eventService;
    private final InitiativeService initiativeService;
    private final JoinRequestService joinRequestService;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;

    public CoordinatorAttendController(AttendService attendService, EventService eventService,
            InitiativeService initiativeService, JoinRequestService joinRequestService,
            VolunteerInitiativeRepository volunteerInitiativeRepository) {
        this.attendService = attendService;
        this.eventService = eventService;
        this.initiativeService = initiativeService;
        this.joinRequestService = joinRequestService;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
    }

    @GetMapping
    public String list(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        addPageModel(model, initiative, eventOf(initiative, eventId));
        model.addAttribute("attendances", attendService.findAllForEvent(eventId));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        addFormModel(model, initiative, eventOf(initiative, eventId), null);
        model.addAttribute("attendForm", new AttendForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("attendForm") AttendForm form, BindingResult bindingResult, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        Event event = eventOf(initiative, eventId);
        AttendanceRules.rejectIfNotApprovedMember(form, initiativeId, bindingResult, volunteerInitiativeRepository);
        if (bindingResult.hasErrors()) {
            addFormModel(model, initiative, event, null);
            return FORM_VIEW;
        }
        attendService.create(eventId, form);
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long initiativeId, @PathVariable Long eventId, @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        Event event = eventOf(initiative, eventId);
        Attend attend = attendanceOf(event, id);

        AttendForm form = new AttendForm();
        form.setAttendInOut(attend.getAttendInOut());
        form.setAttendDttm(attend.getAttendDttm());
        form.setNote(attend.getNote());
        if (attend.getVolunteerInitiative() != null) {
            form.setVolunteerInitiativeId(attend.getVolunteerInitiative().getId());
        }

        addFormModel(model, initiative, event, id);
        model.addAttribute("attendForm", form);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long initiativeId, @PathVariable Long eventId, @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("attendForm") AttendForm form, BindingResult bindingResult, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        Event event = eventOf(initiative, eventId);
        attendanceOf(event, id);
        AttendanceRules.rejectIfNotApprovedMember(form, initiativeId, bindingResult, volunteerInitiativeRepository);
        if (bindingResult.hasErrors()) {
            addFormModel(model, initiative, event, id);
            return FORM_VIEW;
        }
        attendService.update(id, form);
        return REDIRECT_TO_LIST;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long initiativeId, @PathVariable Long eventId, @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        attendanceOf(eventOf(initiative, eventId), id);
        attendService.delete(id);
        return REDIRECT_TO_LIST;
    }

    private Initiative managedInitiative(Long initiativeId, UserPrincipal principal) {
        Initiative initiative = initiativeService.findById(initiativeId);
        CoordinatorAccess.assertCanManage(initiative, principal, joinRequestService);
        return initiative;
    }

    /** The event, checked to belong to the initiative in the URL. */
    private Event eventOf(Initiative initiative, Long eventId) {
        Event event = eventService.findById(eventId);
        if (event.getInitiative() == null || !event.getInitiative().getId().equals(initiative.getId())) {
            throw new AccessDeniedException("Event " + eventId + " does not belong to initiative " + initiative.getId());
        }
        return event;
    }

    /** The attendance record, checked to belong to the event in the URL. */
    private Attend attendanceOf(Event event, Long attendId) {
        Attend attend = attendService.findById(attendId);
        if (attend.getEvent() == null || !attend.getEvent().getId().equals(event.getId())) {
            throw new AccessDeniedException("Attendance " + attendId + " does not belong to event " + event.getId());
        }
        return attend;
    }

    private void addPageModel(Model model, Initiative initiative, Event event) {
        String eventsPath = "/coordinator/initiatives/" + initiative.getId() + "/events";
        model.addAttribute("initiative", initiative);
        model.addAttribute("event", event);
        model.addAttribute("eventsPath", eventsPath);
        model.addAttribute("attendancePath", eventsPath + "/" + event.getId() + "/attendance");
    }

    private void addFormModel(Model model, Initiative initiative, Event event, Long attendId) {
        addPageModel(model, initiative, event);
        model.addAttribute("attendId", attendId);
        model.addAttribute("volunteerInitiatives",
                volunteerInitiativeRepository.findByInitiativeIdAndEnabledTrue(initiative.getId()));
    }
}
