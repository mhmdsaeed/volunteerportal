package com.volunteerportal.app.service;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.volunteerportal.app.model.VolunteerInitiative;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipFilterTest {

    private static VolunteerInitiative membership(LocalDateTime respondedAt, Boolean enabled) {
        VolunteerInitiative membership = new VolunteerInitiative();
        membership.setRequestJoinDttm(LocalDateTime.of(2026, 10, 1, 9, 0));
        membership.setResponseJoinDttm(respondedAt);
        membership.setEnabled(enabled);
        return membership;
    }

    private static final VolunteerInitiative PENDING = membership(null, null);
    private static final VolunteerInitiative APPROVED = membership(LocalDateTime.of(2026, 10, 2, 9, 0), true);
    private static final VolunteerInitiative REJECTED = membership(LocalDateTime.of(2026, 10, 2, 9, 0), false);

    @Test
    void joined_matchesOnlyApprovedMemberships() {
        assertThat(MembershipFilter.JOINED.matches(APPROVED)).isTrue();
        assertThat(MembershipFilter.JOINED.matches(PENDING)).isFalse();
        assertThat(MembershipFilter.JOINED.matches(REJECTED)).isFalse();
        assertThat(MembershipFilter.JOINED.matches(null)).isFalse();
    }

    @Test
    void pending_matchesOnlyUnansweredRequests() {
        assertThat(MembershipFilter.PENDING.matches(PENDING)).isTrue();
        assertThat(MembershipFilter.PENDING.matches(APPROVED)).isFalse();
        assertThat(MembershipFilter.PENDING.matches(REJECTED)).isFalse();
        assertThat(MembershipFilter.PENDING.matches(null)).isFalse();
    }

    @Test
    void rejected_matchesOnlyTurnedDownRequests() {
        assertThat(MembershipFilter.REJECTED.matches(REJECTED)).isTrue();
        assertThat(MembershipFilter.REJECTED.matches(membership(LocalDateTime.of(2026, 10, 2, 9, 0), null))).isTrue();
        assertThat(MembershipFilter.REJECTED.matches(APPROVED)).isFalse();
        assertThat(MembershipFilter.REJECTED.matches(PENDING)).isFalse();
        assertThat(MembershipFilter.REJECTED.matches(null)).isFalse();
    }

    @Test
    void notJoined_matchesOnlyInitiativesWithoutARequest() {
        assertThat(MembershipFilter.NOT_JOINED.matches(null)).isTrue();
        assertThat(MembershipFilter.NOT_JOINED.matches(PENDING)).isFalse();
        assertThat(MembershipFilter.NOT_JOINED.matches(APPROVED)).isFalse();
        assertThat(MembershipFilter.NOT_JOINED.matches(REJECTED)).isFalse();
    }

    @Test
    void all_matchesEverything() {
        assertThat(MembershipFilter.ALL.matches(null)).isTrue();
        assertThat(MembershipFilter.ALL.matches(PENDING)).isTrue();
        assertThat(MembershipFilter.ALL.matches(APPROVED)).isTrue();
        assertThat(MembershipFilter.ALL.matches(REJECTED)).isTrue();
    }

    @Test
    void from_readsTheParameterAndDefaultsToJoined() {
        assertThat(MembershipFilter.from("all")).isEqualTo(MembershipFilter.ALL);
        assertThat(MembershipFilter.from("notJoined")).isEqualTo(MembershipFilter.NOT_JOINED);
        assertThat(MembershipFilter.from("NOTJOINED")).isEqualTo(MembershipFilter.NOT_JOINED);
        assertThat(MembershipFilter.from("pending")).isEqualTo(MembershipFilter.PENDING);
        assertThat(MembershipFilter.from("rejected")).isEqualTo(MembershipFilter.REJECTED);
        assertThat(MembershipFilter.from(null)).isEqualTo(MembershipFilter.JOINED);
        assertThat(MembershipFilter.from("bogus")).isEqualTo(MembershipFilter.JOINED);
    }
}
