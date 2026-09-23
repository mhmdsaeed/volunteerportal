package com.volunteerportal.app.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rotating codes for QR self check-in. A code is {@code <window>-<signature>}, where window is the
 * current time divided into {@code code-period} slots and the signature is an HMAC of the event id
 * and window. A code is accepted for {@code code-max-age}, so an old screenshot soon stops working.
 */
@Component
public class CheckInCodes {

    private static final String HMAC = "HmacSHA256";
    private static final int SIGNATURE_BYTES = 16;

    private final byte[] secret;
    private final long periodSeconds;
    private final long maxAgeWindows;
    private final Clock clock;

    @Autowired
    public CheckInCodes(@Value("${app.checkin.secret:}") String secret,
            @Value("${app.checkin.code-period:30s}") Duration period,
            @Value("${app.checkin.code-max-age:120s}") Duration maxAge) {
        this(secret, period, maxAge, Clock.systemUTC());
    }

    CheckInCodes(String secret, Duration period, Duration maxAge, Clock clock) {
        this.secret = secret == null || secret.isBlank() ? randomSecret() : secret.getBytes(StandardCharsets.UTF_8);
        this.periodSeconds = Math.max(1, period.toSeconds());
        this.maxAgeWindows = Math.max(0, maxAge.toSeconds() / this.periodSeconds);
        this.clock = clock;
    }

    /** The code to show on the coordinator's screen right now. */
    public String currentCode(Long eventId) {
        long window = currentWindow();
        return window + "-" + sign(eventId, window);
    }

    public long periodSeconds() {
        return periodSeconds;
    }

    /** Whether the code was issued for this event within the accepted age. */
    public boolean isValid(Long eventId, String code) {
        if (eventId == null || code == null) {
            return false;
        }
        int dash = code.indexOf('-');
        if (dash <= 0) {
            return false;
        }
        long window;
        try {
            window = Long.parseLong(code.substring(0, dash));
        } catch (NumberFormatException e) {
            return false;
        }
        long age = currentWindow() - window;
        if (age < 0 || age > maxAgeWindows) {
            return false;
        }
        byte[] expected = sign(eventId, window).getBytes(StandardCharsets.US_ASCII);
        byte[] given = code.substring(dash + 1).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, given);
    }

    private long currentWindow() {
        return clock.instant().getEpochSecond() / periodSeconds;
    }

    private String sign(Long eventId, long window) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            byte[] digest = mac.doFinal((eventId + ":" + window).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, SIGNATURE_BYTES));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static byte[] randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}
