package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.OfficeForm;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.OfficeRepository;
import com.volunteerportal.app.repository.RoleRepository;
import com.volunteerportal.app.repository.UserRepository;

@Service
public class OfficeServiceImpl implements OfficeService {

    private final OfficeRepository officeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public OfficeServiceImpl(OfficeRepository officeRepository, RoleRepository roleRepository,
            UserRepository userRepository) {
        this.officeRepository = officeRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<Office> findAll() {
        return officeRepository.findAll();
    }

    @Override
    public Office findById(Long id) {
        return officeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Office not found: " + id));
    }

    @Override
    @Transactional
    public Office create(OfficeForm form) {
        Office office = new Office();
        applyForm(office, form);
        return officeRepository.save(office);
    }

    @Override
    @Transactional
    public Office update(Long id, OfficeForm form) {
        Office office = findById(id);
        applyForm(office, form);
        return officeRepository.save(office);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        officeRepository.deleteById(id);
    }

    private void applyForm(Office office, OfficeForm form) {
        office.setName(form.getName());
        office.setDescription(form.getDescription());

        if (form.getRoleId() != null) {
            Role role = roleRepository.findById(form.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Role not found: " + form.getRoleId()));
            office.setRole(role);
        } else {
            office.setRole(null);
        }

        if (form.getUserId() != null) {
            User user = userRepository.findById(form.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + form.getUserId()));
            office.setUser(user);
        } else {
            office.setUser(null);
        }
    }
}
