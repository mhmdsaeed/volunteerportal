package com.volunteerportal.app.dto;

/**
 * How many volunteers an initiative has: approved members, and join requests still awaiting a decision (the
 * same rules as the Joined and Pending filters, see MembershipFilter).
 */
public record MemberCounts(Long initiativeId, Long members, Long pending) {
}
