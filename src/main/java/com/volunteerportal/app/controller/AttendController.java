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

import com.volunteerportal.app.dto.AttendForm;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;
import com.volunteerportal.app.service.AttendService;
import com.volunteerportal.app.service.EventService;
import com.volunteerportal.app.service.InitiativeService;

@Controller
@RequestMapping("/admin/initiatives/{initiativeId}/events/{eventId}/attendance")
public class AttendController {

    private final AttendService attendService;
    private final EventService eventService;
    private final InitiativeService initiativeService;
    private final VolunteerInitiativeRepository volunteerInitiativeRepository;

    public AttendController(AttendService attendService, EventService eventService,
            InitiativeService initiativeService, VolunteerInitiativeRepository volunteerInitiativeRepository) {
        this.attendService = attendService;
        this.eventService = eventService;
        this.initiativeService = initiativeService;
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
    }

    @GetMapping
    public String list(@PathVariable Long initiativeId, @PathVariable Long eventId, Model model) {
        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("attendances", attendService.findAllForEvent(eventId));
        return "admin/initiatives/events/attendance/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long initiativeId, @PathVariable Long eventId, Model model) {
        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("attendForm", new AttendForm());
        addReferenceData(model, initiativeId);
        return "admin/initiatives/events/attendance/form";
    }

    @PostMapping
    public String create(@PathVariable Long initiativeId, @PathVariable Long eventId,
            @Valid @ModelAttribute("attendForm") AttendForm form, BindingResult bindingResult, Model model) {
        rejectIfNotApprovedMember(form, initiativeId, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiative", initiativeService.findById(initiativeId));
            model.addAttribute("event", eventService.findById(eventId));
            addReferenceData(model, initiativeId);
            return "admin/initiatives/events/attendance/form";
        }
        attendService.create(eventId, form);
        return "redirect:/admin/initiatives/{initiativeId}/events/{eventId}/attendance";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long initiativeId, @PathVariable Long eventId, @PathVariable Long id,
            Model model) {
        var attend = attendService.findById(id);

        AttendForm form = new AttendForm();
        form.setAttendInOut(attend.getAttendInOut());
        form.setAttendDttm(attend.getAttendDttm());
        form.setNote(attend.getNote());
        if (attend.getVolunteerInitiative() != null) {
            form.setVolunteerInitiativeId(attend.getVolunteerInitiative().getId());
        }

        model.addAttribute("initiative", initiativeService.findById(initiativeId));
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("attendForm", form);
        model.addAttribute("attendId", id);
        addReferenceData(model, initiativeId);
        return "admin/initiatives/events/attendance/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long initiativeId, @PathVariable Long eventId, @PathVariable Long id,
            @Valid @ModelAttribute("attendForm") AttendForm form, BindingResult bindingResult, Model model) {
        rejectIfNotApprovedMember(form, initiativeId, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiative", initiativeService.findById(initiativeId));
            model.addAttribute("event", eventService.findById(eventId));
            model.addAttribute("attendId", id);
            addReferenceData(model, initiativeId);
            return "admin/initiatives/events/attendance/form";
        }
        attendService.update(id, form);
        return "redirect:/admin/initiatives/{initiativeId}/events/{eventId}/attendance";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long initiativeId, @PathVariable Long eventId, @PathVariable Long id) {
        attendService.delete(id);
        return "redirect:/admin/initiatives/{initiativeId}/events/{eventId}/attendance";
    }

    private void addReferenceData(Model model, Long initiativeId) {
        model.addAttribute("volunteerInitiatives", volunteerInitiativeRepository.findByInitiativeIdAndEnabledTrue(initiativeId));
    }

    private void rejectIfNotApprovedMember(AttendForm form, Long initiativeId, BindingResult bindingResult) {
        AttendanceRules.rejectIfNotApprovedMember(form, initiativeId, bindingResult, volunteerInitiativeRepository);
    }
}
