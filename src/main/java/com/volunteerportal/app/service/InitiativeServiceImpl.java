package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.InitiativeForm;
import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.UserRepository;

@Service
public class InitiativeServiceImpl implements InitiativeService {

    private final InitiativeRepository initiativeRepository;
    private final OfficeRepository officeRepository;
    private final UserRepository userRepository;

    public InitiativeServiceImpl(InitiativeRepository initiativeRepository, OfficeRepository officeRepository,
            UserRepository userRepository) {
        this.initiativeRepository = initiativeRepository;
        this.officeRepository = officeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Initiative> findAll() {
        return initiativeRepository.findAll();
    }

    @Override
    public Initiative findById(Long id) {
        return initiativeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Initiative not found: " + id));
    }

    @Override
    @Transactional
    public Initiative create(InitiativeForm form) {
        Initiative initiative = new Initiative();
        applyForm(initiative, form);
        return initiativeRepository.save(initiative);
    }

    @Override
    @Transactional
    public Initiative update(Long id, InitiativeForm form) {
        Initiative initiative = findById(id);
        applyForm(initiative, form);
        return initiativeRepository.save(initiative);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        initiativeRepository.deleteById(id);
    }

    private void applyForm(Initiative initiative, InitiativeForm form) {
        initiative.setName(form.getName());
        initiative.setDescription(form.getDescription());
        initiative.setEnabled(form.isEnabled());

        if (form.getOfficeId() != null) {
            Office office = officeRepository.findById(form.getOfficeId())
                    .orElseThrow(() -> new EntityNotFoundException("Office not found: " + form.getOfficeId()));
            initiative.setOffice(office);
        } else {
            initiative.setOffice(null);
        }

        if (form.getSupervisorId() != null) {
            User supervisor = userRepository.findById(form.getSupervisorId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + form.getSupervisorId()));
            initiative.setSupervisor(supervisor);
        } else {
            initiative.setSupervisor(null);
        }
    }
}
