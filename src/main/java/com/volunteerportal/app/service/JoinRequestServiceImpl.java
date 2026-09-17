package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.repository.InitiativeRepository;
import com.volunteerportal.app.repository.VolunteerInitiativeRepository;

@Service
public class JoinRequestServiceImpl implements JoinRequestService {

    private final VolunteerInitiativeRepository volunteerInitiativeRepository;
    private final InitiativeRepository initiativeRepository;
    private final NotificationService notificationService;

    public JoinRequestServiceImpl(VolunteerInitiativeRepository volunteerInitiativeRepository,
            InitiativeRepository initiativeRepository, NotificationService notificationService) {
        this.volunteerInitiativeRepository = volunteerInitiativeRepository;
        this.initiativeRepository = initiativeRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<Initiative> findManagedInitiatives(Long supervisorId) {
        return supervisorId == null ? initiativeRepository.findAll()
                : initiativeRepository.findBySupervisorId(supervisorId);
    }

    @Override
    public List<VolunteerInitiative> findRequestsForInitiative(Long initiativeId) {
        return volunteerInitiativeRepository.findByInitiativeId(initiativeId);
    }

    @Override
    public VolunteerInitiative findById(Long id) {
        return volunteerInitiativeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Join request not found: " + id));
    }

    @Override
    @Transactional
    public VolunteerInitiative approve(Long id) {
        VolunteerInitiative request = findById(id);
        request.setResponseJoinDttm(LocalDateTime.now());
        request.setEnabled(true);
        VolunteerInitiative saved = volunteerInitiativeRepository.save(request);

        notificationService.notify(saved.getUser(),
                "Your request to join '" + saved.getInitiative().getName() + "' was approved.",
                "/initiatives/" + saved.getInitiative().getId());
        return saved;
    }

    @Override
    @Transactional
    public VolunteerInitiative reject(Long id) {
        VolunteerInitiative request = findById(id);
        request.setResponseJoinDttm(LocalDateTime.now());
        request.setEnabled(false);
        VolunteerInitiative saved = volunteerInitiativeRepository.save(request);

        notificationService.notify(saved.getUser(),
                "Your request to join '" + saved.getInitiative().getName() + "' was not approved.",
                "/initiatives/" + saved.getInitiative().getId());
        return saved;
    }
}
