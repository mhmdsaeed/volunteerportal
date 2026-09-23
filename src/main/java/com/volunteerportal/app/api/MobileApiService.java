package com.volunteerportal.app.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.volunteerportal.app.api.ApiDtos.AttendanceItem;
import com.volunteerportal.app.api.ApiDtos.CheckInResponse;
import com.volunteerportal.app.api.ApiDtos.EventItem;
import com.volunteerportal.app.api.ApiDtos.InitiativeItem;
import com.volunteerportal.app.api.ApiDtos.Me;
import com.volunteerportal.app.api.ApiDtos.NotificationItem;
import com.volunteerportal.app.model.Event;
import com.volunteerportal.app.model.Notification;
import com.volunteerportal.app.model.Role;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.model.VolunteerProfile;
import com.volunteerportal.app.repository.AttendRepository;
import com.volunteerportal.app.repository.EventRepository;
import com.volunteerportal.app.repository.VolunteerProfileRepository;
import com.volunteerportal.app.service.CheckInService;
import com.volunteerportal.app.service.NotificationService;
import com.volunteerportal.app.service.VolunteerInitiativeService;

/**
 * Builds the mobile API's responses. Mapping happens inside read-only transactions because lazy
 * associations can't be read in controllers (open-in-view is disabled). Business rules stay in
 * the existing services (check-in, notifications, memberships) shared with the website.
 */
@Service
public class MobileApiService {

    /** The path of the check-in link inside the QR code: /checkin/{eventId}?code=... */
    private static final Pattern CHECKIN_PATH = Pattern.compile("^/checkin/(\\d+)/?$");

    private final VolunteerInitiativeService volunteerInitiativeService;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final EventRepository eventRepository;
    private final AttendRepository attendRepository;
    private final CheckInService checkInService;
    private final NotificationService notificationService;
    private final MessageSource messageSource;

    public MobileApiService(VolunteerInitiativeService volunteerInitiativeService,
            VolunteerProfileRepository volunteerProfileRepository, EventRepository eventRepository,
            AttendRepository attendRepository, CheckInService checkInService,
            NotificationService notificationService, MessageSource messageSource) {
        this.volunteerInitiativeService = volunteerInitiativeService;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.eventRepository = eventRepository;
        this.attendRepository = attendRepository;
        this.checkInService = checkInService;
        this.notificationService = notificationService;
        this.messageSource = messageSource;
    }

    /** A scanned QR code's event id and code, if it is a check-in link. */
    public record ScannedCode(Long eventId, String code) {
    }

    @Transactional(readOnly = true)
    public Me me(User user) {
        Optional<VolunteerProfile> profile = volunteerProfileRepository.findByUserId(user.getId());
        return new Me(user.getId(), user.getUsername(), user.getEmail(),
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                profile.map(VolunteerProfile::getGrade).map(g -> g.getName()).orElse(null),
                profile.map(VolunteerProfile::getPoints).orElse(0L));
    }

    @Transactional(readOnly = true)
    public List<InitiativeItem> initiatives(Long userId) {
        Map<Long, VolunteerInitiative> memberships = volunteerInitiativeService.findMembershipsForUser(userId);
        return volunteerInitiativeService.findAvailableInitiatives().stream()
                .map(i -> new InitiativeItem(i.getId(), i.getName(), i.getDescription(),
                        i.getOffice() != null ? i.getOffice().getName() : null,
                        membershipStatus(memberships.get(i.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventItem> upcomingEvents(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findUpcomingForMember(userId, now, LocalDate.now().atStartOfDay()).stream()
                .map(e -> new EventItem(e.getId(), e.getName(), e.getInitiative().getId(), e.getInitiative().getName(),
                        e.getFromDttm(), e.getToDttm(), e.getLocUrl(), e.getLocLatitude(), e.getLocLongitude(),
                        checkInService.requiresLocation(e), myStatus(checkInService.nextAction(e, userId))))
                .toList();
    }

    @Transactional
    public CheckInResponse checkIn(Long userId, ScannedCode scanned, Double latitude, Double longitude, Locale locale) {
        CheckInService.Result result = checkInService.checkIn(scanned.eventId(), userId, scanned.code(), latitude, longitude);
        Event event = checkInService.findEvent(scanned.eventId());
        boolean success = result == CheckInService.Result.CHECKED_IN || result == CheckInService.Result.CHECKED_OUT;
        return new CheckInResponse(result.name(), success, event.getId(), event.getName(),
                messageSource.getMessage("checkin.result." + result.name(), null, locale));
    }

    @Transactional(readOnly = true)
    public List<AttendanceItem> attendance(Long userId) {
        return attendRepository.findByVolunteerInitiative_User_IdOrderByAttendDttmDesc(userId).stream()
                .map(a -> new AttendanceItem(a.getId(), a.getEvent().getId(), a.getEvent().getName(),
                        a.getEvent().getInitiative() != null ? a.getEvent().getInitiative().getName() : null,
                        Integer.valueOf(2).equals(a.getAttendInOut()) ? "CHECK_OUT" : "CHECK_IN",
                        a.getAttendDttm(), a.getNote()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationItem> notifications(Long userId, Locale locale) {
        return notificationService.findForUser(userId).stream()
                .map(n -> new NotificationItem(n.getId(), text(n, locale), n.getLink(), n.isRead(), n.getCreatedDttm()))
                .toList();
    }

    /**
     * Reads a scanned QR code: the app sends the text as-is and the server takes the event id and
     * code from the check-in link (any host, since it may be opened on a different address).
     */
    public static Optional<ScannedCode> parseQr(String qr) {
        if (qr == null || qr.isBlank()) {
            return Optional.empty();
        }
        try {
            UriComponents uri = UriComponentsBuilder.fromUriString(qr.trim()).build();
            Matcher path = CHECKIN_PATH.matcher(uri.getPath() == null ? "" : uri.getPath());
            String code = uri.getQueryParams().getFirst("code");
            if (!path.matches() || code == null || code.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ScannedCode(Long.valueOf(path.group(1)), code));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String text(Notification notification, Locale locale) {
        if (notification.getMessageKey() == null) {
            return notification.getMessage(); // older notifications only have their stored text
        }
        return messageSource.getMessage(notification.getMessageKey(), notification.getMessageArgs().toArray(),
                notification.getMessage(), locale);
    }

    private static String membershipStatus(VolunteerInitiative membership) {
        if (membership == null) {
            return "NONE";
        }
        if (membership.getResponseJoinDttm() == null) {
            return "PENDING";
        }
        return Boolean.TRUE.equals(membership.getEnabled()) ? "APPROVED" : "REJECTED";
    }

    private static String myStatus(CheckInService.Action action) {
        return switch (action) {
            case CHECK_OUT -> "CHECKED_IN";
            case DONE -> "CHECKED_OUT";
            default -> "NOT_CHECKED_IN";
        };
    }
}
