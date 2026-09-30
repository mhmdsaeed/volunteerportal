package com.volunteerportal.app.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.volunteerportal.app.dto.InitiativeForm;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.InitiativeService;

@Controller
@RequestMapping("/admin/initiatives")
public class InitiativeController {

    /** Who may supervise an initiative. */
    static final List<String> SUPERVISOR_ROLES = List.of("ADMIN", "COORDINATOR");

    private final InitiativeService initiativeService;
    private final OfficeRepository officeRepository;
    private final UserRepository userRepository;

    public InitiativeController(InitiativeService initiativeService, OfficeRepository officeRepository,
            UserRepository userRepository) {
        this.initiativeService = initiativeService;
        this.officeRepository = officeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("initiatives", initiativeService.findAll());
        return "admin/initiatives/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("initiativeForm", new InitiativeForm());
        addReferenceData(model, null);
        return "admin/initiatives/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("initiativeForm") InitiativeForm form, BindingResult bindingResult,
            Model model) {
        rejectIfNotEligibleSupervisor(form, bindingResult, null);
        if (bindingResult.hasErrors()) {
            addReferenceData(model, null);
            return "admin/initiatives/form";
        }
        initiativeService.create(form);
        return "redirect:/admin/initiatives";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var initiative = initiativeService.findById(id);

        InitiativeForm form = new InitiativeForm();
        form.setName(initiative.getName());
        form.setDescription(initiative.getDescription());
        form.setEnabled(Boolean.TRUE.equals(initiative.getEnabled()));
        if (initiative.getOffice() != null) {
            form.setOfficeId(initiative.getOffice().getId());
        }
        if (initiative.getSupervisor() != null) {
            form.setSupervisorId(initiative.getSupervisor().getId());
        }

        model.addAttribute("initiativeForm", form);
        model.addAttribute("initiativeId", id);
        addReferenceData(model, form.getSupervisorId());
        return "admin/initiatives/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("initiativeForm") InitiativeForm form,
            BindingResult bindingResult, Model model) {
        Long currentSupervisorId = currentSupervisorId(id);
        rejectIfNotEligibleSupervisor(form, bindingResult, currentSupervisorId);
        if (bindingResult.hasErrors()) {
            model.addAttribute("initiativeId", id);
            addReferenceData(model, currentSupervisorId);
            return "admin/initiatives/form";
        }
        initiativeService.update(id, form);
        return "redirect:/admin/initiatives";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        initiativeService.delete(id);
        return "redirect:/admin/initiatives";
    }

    /**
     * Supervisor choices: active admins and coordinators. When editing, the current supervisor stays
     * listed (marked) even if they no longer qualify, so saving the form doesn't silently remove them.
     */
    private void addReferenceData(Model model, Long currentSupervisorId) {
        model.addAttribute("offices", officeRepository.findAll());
        List<User> supervisors = new ArrayList<>(userRepository.findActiveWithAnyRole(SUPERVISOR_ROLES));
        Set<Long> notEligible = new HashSet<>();
        if (currentSupervisorId != null && supervisors.stream().noneMatch(u -> u.getId().equals(currentSupervisorId))) {
            userRepository.findById(currentSupervisorId).ifPresent(current -> {
                supervisors.add(current);
                notEligible.add(current.getId());
            });
        }
        model.addAttribute("users", supervisors);
        model.addAttribute("notEligibleSupervisorIds", notEligible);
    }

    /** The supervisor must be an active admin or coordinator (unless it's the initiative's unchanged current one). */
    private void rejectIfNotEligibleSupervisor(InitiativeForm form, BindingResult bindingResult, Long currentSupervisorId) {
        Long chosen = form.getSupervisorId();
        if (chosen == null || chosen.equals(currentSupervisorId)) {
            return;
        }
        boolean eligible = userRepository.findActiveWithAnyRole(SUPERVISOR_ROLES).stream()
                .anyMatch(u -> u.getId().equals(chosen));
        if (!eligible) {
            bindingResult.rejectValue("supervisorId", "error.supervisor.notEligible",
                    "The supervisor must be an active admin or coordinator");
        }
    }

    private Long currentSupervisorId(Long initiativeId) {
        var supervisor = initiativeService.findById(initiativeId).getSupervisor();
        return supervisor != null ? supervisor.getId() : null;
    }
}
