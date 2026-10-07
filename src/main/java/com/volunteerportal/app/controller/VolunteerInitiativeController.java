package com.volunteerportal.app.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.volunteerportal.app.model.Initiative;
import com.volunteerportal.app.model.Office;
import com.volunteerportal.app.model.VolunteerInitiative;
import com.volunteerportal.app.security.UserPrincipal;
import com.volunteerportal.app.service.MembershipFilter;
import com.volunteerportal.app.service.VolunteerInitiativeService;

@Controller
@RequestMapping("/initiatives")
public class VolunteerInitiativeController {

    /** The `office` value for initiatives that belong to no office. */
    static final String NO_OFFICE = "none";

    private static final Comparator<Office> OFFICE_ORDER =
            Comparator.comparing(o -> o.getName() == null ? "" : o.getName(), String.CASE_INSENSITIVE_ORDER);

    /** Initiatives of one office (office null: those without one), as the list shows them under a heading. */
    public record OfficeGroup(Office office, List<Initiative> initiatives) {
    }

    private final VolunteerInitiativeService volunteerInitiativeService;

    public VolunteerInitiativeController(VolunteerInitiativeService volunteerInitiativeService) {
        this.volunteerInitiativeService = volunteerInitiativeService;
    }

    /**
     * The open initiatives, grouped by office. `show` filters by my membership (default: joined) and `office`
     * by office (an office id, or "none"; anything else means all offices). The membership counts are for the
     * chosen office, so they always add up to what the page can show.
     */
    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "show", required = false) String show,
            @RequestParam(name = "office", required = false) String office, Model model) {
        MembershipFilter filter = MembershipFilter.from(show);
        List<Initiative> available = volunteerInitiativeService.findAvailableInitiatives();

        // The offices to choose from: those with an open initiative, by name
        List<Office> offices = available.stream().map(Initiative::getOffice).filter(Objects::nonNull)
                .collect(Collectors.toMap(Office::getId, o -> o, (a, b) -> a, LinkedHashMap::new))
                .values().stream().sorted(OFFICE_ORDER).toList();
        boolean anyWithoutOffice = available.stream().anyMatch(i -> i.getOffice() == null);
        String officeParam = validOffice(office, offices, anyWithoutOffice);

        List<Initiative> initiatives = available.stream().filter(i -> inOffice(i, officeParam)).toList();
        Map<Long, VolunteerInitiative> memberships =
                volunteerInitiativeService.findMembershipsForUser(principal.getUser().getId());

        Map<MembershipFilter, Long> counts = new EnumMap<>(MembershipFilter.class);
        for (MembershipFilter option : MembershipFilter.values()) {
            counts.put(option, initiatives.stream().filter(i -> option.matches(memberships.get(i.getId()))).count());
        }

        List<Initiative> shown = initiatives.stream().filter(i -> filter.matches(memberships.get(i.getId()))).toList();
        model.addAttribute("initiatives", shown);
        model.addAttribute("groups", groupByOffice(shown));
        model.addAttribute("offices", offices);
        model.addAttribute("anyWithoutOffice", anyWithoutOffice);
        model.addAttribute("office", officeParam);
        model.addAttribute("noOffice", NO_OFFICE);
        model.addAttribute("memberships", memberships);
        model.addAttribute("filter", filter);
        model.addAttribute("filters", MembershipFilter.values());
        model.addAttribute("counts", counts);
        return "initiatives/list";
    }

    /** The office filter if it names an office in the list (or "none" when some have no office), else null: all. */
    private static String validOffice(String office, List<Office> offices, boolean anyWithoutOffice) {
        if (NO_OFFICE.equals(office)) {
            return anyWithoutOffice ? NO_OFFICE : null;
        }
        return offices.stream().map(o -> String.valueOf(o.getId())).filter(id -> id.equals(office))
                .findFirst().orElse(null);
    }

    private static boolean inOffice(Initiative initiative, String office) {
        if (office == null) {
            return true;
        }
        if (NO_OFFICE.equals(office)) {
            return initiative.getOffice() == null;
        }
        return initiative.getOffice() != null && office.equals(String.valueOf(initiative.getOffice().getId()));
    }

    /** One group per office, by office name; initiatives without an office come last. */
    static List<OfficeGroup> groupByOffice(List<Initiative> initiatives) {
        Map<Long, OfficeGroup> byOffice = new LinkedHashMap<>();
        List<Initiative> withoutOffice = new ArrayList<>();
        for (Initiative initiative : initiatives) {
            Office office = initiative.getOffice();
            if (office == null) {
                withoutOffice.add(initiative);
            } else {
                byOffice.computeIfAbsent(office.getId(), id -> new OfficeGroup(office, new ArrayList<>()))
                        .initiatives().add(initiative);
            }
        }
        List<OfficeGroup> groups = new ArrayList<>(byOffice.values().stream()
                .sorted(Comparator.comparing(OfficeGroup::office, OFFICE_ORDER)).toList());
        if (!withoutOffice.isEmpty()) {
            groups.add(new OfficeGroup(null, withoutOffice));
        }
        return groups;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        VolunteerInitiative membership =
                volunteerInitiativeService.findMembership(principal.getUser().getId(), id).orElse(null);
        boolean approvedMember = membership != null && Boolean.TRUE.equals(membership.getEnabled());

        model.addAttribute("initiative", volunteerInitiativeService.findInitiativeDetail(id));
        model.addAttribute("questions", volunteerInitiativeService.findQuestions(id));
        model.addAttribute("membership", membership);
        // Events are only for members: a volunteer can attend once their join request is approved
        model.addAttribute("events", approvedMember ? volunteerInitiativeService.findOpenEvents(id) : List.of());
        return "initiatives/detail";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam MultiValueMap<String, String> formParams) {
        volunteerInitiativeService.join(id, principal.getUser(), formParams);
        return "redirect:/initiatives/{id}";
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        volunteerInitiativeService.withdraw(id, principal.getUser().getId());
        return "redirect:/initiatives/{id}";
    }
}
