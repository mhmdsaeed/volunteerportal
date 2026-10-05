package com.volunteerportal.app.service;

import com.volunteerportal.app.model.VolunteerInitiative;

/**
 * The options of the volunteer's initiatives list (`/initiatives?show=...`), by the volunteer's own
 * membership in each initiative. Opens on {@link #JOINED}.
 */
public enum MembershipFilter {

    JOINED("joined"),
    PENDING("pending"),
    REJECTED("rejected"),
    NOT_JOINED("notJoined"),
    ALL("all");

    private final String param;

    MembershipFilter(String param) {
        this.param = param;
    }

    /** The value of the `show` request parameter, also the suffix of its `initiatives.filter.*` label. */
    public String getParam() {
        return param;
    }

    /** The filter named by a `show` parameter; a missing or unknown value means {@link #JOINED}. */
    public static MembershipFilter from(String param) {
        for (MembershipFilter filter : values()) {
            if (filter.param.equalsIgnoreCase(param)) {
                return filter;
            }
        }
        return JOINED;
    }

    /** Whether an initiative with this membership of the volunteer (null when they never asked to join) is shown. */
    public boolean matches(VolunteerInitiative membership) {
        return switch (this) {
            case JOINED -> membership != null && membership.getResponseJoinDttm() != null
                    && Boolean.TRUE.equals(membership.getEnabled());
            case PENDING -> membership != null && membership.getResponseJoinDttm() == null;
            case REJECTED -> membership != null && membership.getResponseJoinDttm() != null
                    && !Boolean.TRUE.equals(membership.getEnabled());
            case NOT_JOINED -> membership == null;
            case ALL -> true;
        };
    }
}
