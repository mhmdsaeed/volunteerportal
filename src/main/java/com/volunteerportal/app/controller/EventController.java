package com.volunteerportal.app.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.dto.EventForm;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;

@Controller
@RequestMapping("/admin/initiatives/{initiativeId}/events")
public class EventController {

    private final EventService eventService;
    private final InitiativeService initiativeService;

    public EventController(EventService eventService, InitiativeService initiativeService) {
        this.eventService = eventService;
        this.initiativeService = initiativeService;
    }

    @GetMapping
    public String list(@PathVariable Long initiativeId, Model model) {
        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("events", eventService.findAllForInitiative(initiativeId));
        return "admin/initiatives/events/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long initiativeId, Model model) {
        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("eventForm", new EventForm());
        return "admin/initiatives/events/form";
    }

    @PostMapping
    public String create(@PathVariable Long initiativeId, @Valid @ModelAttribute("eventForm") EventForm form,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiative", initiativeService.findById(initiativeId));
            return "admin/initiatives/events/form";
        }
        eventService.create(initiativeId, form);
        return "redirect:/admin/initiatives/{initiativeId}/events";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long initiativeId, @PathVariable Long id, Model model) {
        var event = eventService.findById(id);

        EventForm form = new EventForm();
        form.setName(event.getName());
        form.setFromDttm(event.getFromDttm());
        form.setToDttm(event.getToDttm());
        form.setLocLongitude(event.getLocLongitude());
        form.setLocLatitude(event.getLocLatitude());
        form.setLocUrl(event.getLocUrl());
        form.setEnabled(Boolean.TRUE.equals(event.getEnabled()));

        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("eventForm", form);
        model.addAttribute("eventId", id);
        return "admin/initiatives/events/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long initiativeId, @PathVariable Long id,
            @Valid @ModelAttribute("eventForm") EventForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiative", initiativeService.findById(initiativeId));
            model.addAttribute("eventId", id);
            return "admin/initiatives/events/form";
        }
        eventService.update(id, form);
        return "redirect:/admin/initiatives/{initiativeId}/events";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long initiativeId, @PathVariable Long id) {
        eventService.delete(id);
        return "redirect:/admin/initiatives/{initiativeId}/events";
    }
}
