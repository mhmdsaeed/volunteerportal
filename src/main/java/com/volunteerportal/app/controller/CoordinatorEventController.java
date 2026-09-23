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

import com.volunteerportal.app.dto.EventForm;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;
import com.volunteerportal.app.service.JoinRequestService;

/**
 * Lets an initiative's coordinators (its supervisor or its office's coordinator) add and edit the
 * initiative's events; admins can use it for any initiative. Deleting events stays in the admin area.
 */
@Controller
@RequestMapping("/coordinator/initiatives/{initiativeId}/events")
public class CoordinatorEventController {

    private static final String FORM_VIEW = "admin/initiatives/events/form";

    private final EventService eventService;
    private final InitiativeService initiativeService;
    private final JoinRequestService joinRequestService;

    public CoordinatorEventController(EventService eventService, InitiativeService initiativeService,
            JoinRequestService joinRequestService) {
        this.eventService = eventService;
        this.initiativeService = initiativeService;
        this.joinRequestService = joinRequestService;
    }

    @GetMapping
    public String list(@PathVariable Long initiativeId, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("initiative", managedInitiative(initiativeId, principal));
        model.addAttribute("events", eventService.findAllForInitiative(initiativeId));
        return "coordinator/initiatives/events";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long initiativeId, @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        addFormModel(model, managedInitiative(initiativeId, principal), null);
        model.addAttribute("eventForm", new EventForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@PathVariable Long initiativeId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("eventForm") EventForm form, BindingResult bindingResult, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        if (bindingResult.hasErrors()) {
            addFormModel(model, initiative, null);
            return FORM_VIEW;
        }
        eventService.create(initiativeId, form);
        return "redirect:/coordinator/initiatives/{initiativeId}/events";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long initiativeId, @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        Event event = eventOf(initiative, id);

        EventForm form = new EventForm();
        form.setName(event.getName());
        form.setFromDttm(event.getFromDttm());
        form.setToDttm(event.getToDttm());
        form.setLocLongitude(event.getLocLongitude());
        form.setLocLatitude(event.getLocLatitude());
        form.setLocUrl(event.getLocUrl());
        form.setEnabled(Boolean.TRUE.equals(event.getEnabled()));

        addFormModel(model, initiative, id);
        model.addAttribute("eventForm", form);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long initiativeId, @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute("eventForm") EventForm form, BindingResult bindingResult, Model model) {
        Initiative initiative = managedInitiative(initiativeId, principal);
        eventOf(initiative, id);
        if (bindingResult.hasErrors()) {
            addFormModel(model, initiative, id);
            return FORM_VIEW;
        }
        eventService.update(id, form);
        return "redirect:/coordinator/initiatives/{initiativeId}/events";
    }

    private Initiative managedInitiative(Long initiativeId, UserPrincipal principal) {
        Initiative initiative = initiativeService.findById(initiativeId);
        CoordinatorAccess.assertCanManage(initiative, principal, joinRequestService);
        return initiative;
    }

    /** The event, checked to belong to the initiative in the URL (so one can't edit another initiative's event). */
    private Event eventOf(Initiative initiative, Long eventId) {
        Event event = eventService.findById(eventId);
        if (event.getInitiative() == null || !event.getInitiative().getId().equals(initiative.getId())) {
            throw new AccessDeniedException("Event " + eventId + " does not belong to initiative " + initiative.getId());
        }
        return event;
    }

    private void addFormModel(Model model, Initiative initiative, Long eventId) {
        model.addAttribute("initiative", initiative);
        model.addAttribute("eventId", eventId);
        model.addAttribute("eventsPath", "/coordinator/initiatives/" + initiative.getId() + "/events");
    }
}
