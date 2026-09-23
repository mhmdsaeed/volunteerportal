package com.volunteerportal.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckInCodesTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-23T10:00:00Z"));
    private final CheckInCodes codes = new CheckInCodes("test-secret", Duration.ofSeconds(30), Duration.ofSeconds(120), clock);

    @Test
    void currentCode_isValidForItsEvent() {
        assertThat(codes.isValid(5L, codes.currentCode(5L))).isTrue();
    }

    @Test
    void code_changesEveryPeriod() {
        String first = codes.currentCode(5L);
        clock.advance(Duration.ofSeconds(30));

        assertThat(codes.currentCode(5L)).isNotEqualTo(first);
    }

    @Test
    void code_staysValidForMaxAge_thenExpires() {
        String code = codes.currentCode(5L);

        clock.advance(Duration.ofSeconds(120));
        assertThat(codes.isValid(5L, code)).isTrue();

        clock.advance(Duration.ofSeconds(30));
        assertThat(codes.isValid(5L, code)).isFalse();
    }

    @Test
    void code_forAnotherEvent_isRejected() {
        assertThat(codes.isValid(6L, codes.currentCode(5L))).isFalse();
    }

    @Test
    void tamperedOrMalformedCodes_areRejected() {
        String code = codes.currentCode(5L);
        String window = code.substring(0, code.indexOf('-'));

        assertThat(codes.isValid(5L, window + "-AAAAAAAAAAAAAAAAAAAAAA")).isFalse();
        assertThat(codes.isValid(5L, (Long.parseLong(window) + 1) + code.substring(code.indexOf('-')))).isFalse();
        assertThat(codes.isValid(5L, "nonsense")).isFalse();
        assertThat(codes.isValid(5L, "-abc")).isFalse();
        assertThat(codes.isValid(5L, "")).isFalse();
        assertThat(codes.isValid(5L, null)).isFalse();
    }

    @Test
    void codeFromTheFuture_isRejected() {
        clock.advance(Duration.ofMinutes(10));
        String future = codes.currentCode(5L);
        clock.advance(Duration.ofMinutes(-10));

        assertThat(codes.isValid(5L, future)).isFalse();
    }

    @Test
    void differentSecrets_doNotAcceptEachOthersCodes() {
        CheckInCodes other = new CheckInCodes("another-secret", Duration.ofSeconds(30), Duration.ofSeconds(120), clock);

        assertThat(other.isValid(5L, codes.currentCode(5L))).isFalse();
    }

    @Test
    void blankSecret_generatesARandomOne() {
        CheckInCodes first = new CheckInCodes("", Duration.ofSeconds(30), Duration.ofSeconds(120), clock);
        CheckInCodes second = new CheckInCodes(" ", Duration.ofSeconds(30), Duration.ofSeconds(120), clock);

        assertThat(first.isValid(5L, first.currentCode(5L))).isTrue();
        assertThat(second.isValid(5L, first.currentCode(5L))).isFalse();
    }

    /** A clock the test can move forward and back. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
