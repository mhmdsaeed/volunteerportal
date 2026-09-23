package com.volunteerportal.app.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    public List<Initiative> findManagedInitiatives(Long managerId) {
        return managerId == null ? initiativeRepository.findAll()
                : initiativeRepository.findManagedBy(managerId);
    }

    @Override
    public boolean canManage(Initiative initiative, Long userId) {
        if (userId == null) {
            return false;
        }
        Long supervisorId = initiative.getSupervisor() != null ? initiative.getSupervisor().getId() : null;
        Long officeCoordinatorId = initiative.getOffice() != null && initiative.getOffice().getUser() != null
                ? initiative.getOffice().getUser().getId()
                : null;
        return userId.equals(supervisorId) || userId.equals(officeCoordinatorId);
    }

    @Override
    public List<VolunteerInitiative> findRequestsForInitiative(Long initiativeId) {
        return volunteerInitiativeRepository.findByInitiativeId(initiativeId);
    }

    @Override
    public List<VolunteerInitiative> findPendingRequests(Long managerId) {
        return managerId == null ? volunteerInitiativeRepository.findByResponseJoinDttmIsNullOrderByRequestJoinDttmAsc()
                : volunteerInitiativeRepository.findPendingManagedBy(managerId);
    }

    @Override
    public long countPendingRequests(Long managerId) {
        return managerId == null ? volunteerInitiativeRepository.countByResponseJoinDttmIsNull()
                : volunteerInitiativeRepository.countPendingManagedBy(managerId);
    }

    @Override
    public VolunteerInitiative findById(Long id) {
        return volunteerInitiativeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Join request not found: " + id));
    }

    @Override
    @Transactional
    public VolunteerInitiative approve(Long id) {
        VolunteerInitiative saved = decide(id, true);

        notificationService.notify(saved.getUser(),
                "Your request to join '" + saved.getInitiative().getName() + "' was approved.",
                "/initiatives/" + saved.getInitiative().getId());
        return saved;
    }

    @Override
    @Transactional
    public VolunteerInitiative reject(Long id) {
        VolunteerInitiative saved = decide(id, false);

        notificationService.notify(saved.getUser(),
                "Your request to join '" + saved.getInitiative().getName() + "' was not approved.",
                "/initiatives/" + saved.getInitiative().getId());
        return saved;
    }

    private VolunteerInitiative decide(Long id, boolean approved) {
        VolunteerInitiative request = findById(id);
        // Guards against a double submit or two managers deciding the same request (and a second notification)
        if (request.getResponseJoinDttm() != null) {
            throw new IllegalStateException("Join request " + id + " has already been "
                    + (Objects.equals(request.getEnabled(), true) ? "approved" : "rejected"));
        }
        request.setResponseJoinDttm(LocalDateTime.now());
        request.setEnabled(approved);
        return volunteerInitiativeRepository.save(request);
    }
}
