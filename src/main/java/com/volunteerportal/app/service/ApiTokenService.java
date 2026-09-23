package com.volunteerportal.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.ApiToken;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.UserRepository;

/**
 * Bearer tokens for the mobile app: 256 random bits, handed to the app once at login; only their
 * SHA-256 hash is stored. Logging out deletes the row, so a token can be revoked at any time.
 */
@Service
public class ApiTokenService {

    public record IssuedToken(String token, LocalDateTime expiresAt, User user) {
    }

    static final String PREFIX = "vp_";
    private static final int TOKEN_BYTES = 32;
    /** Don't write last_used on every request - once every few minutes is enough. */
    private static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(5);

    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Duration validity;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    // Checked when the username doesn't exist, so a failed login takes as long either way
    private final String dummyHash;

    @Autowired
    public ApiTokenService(ApiTokenRepository apiTokenRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, @Value("${app.api.token-validity:30d}") Duration validity) {
        this(apiTokenRepository, userRepository, passwordEncoder, validity, Clock.systemDefaultZone());
    }

    ApiTokenService(ApiTokenRepository apiTokenRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, Duration validity, Clock clock) {
        this.apiTokenRepository = apiTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validity = validity;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** Checks the credentials and, if they're right and the account is enabled, issues a new token. */
    @Transactional
    public Optional<IssuedToken> login(String username, String password, String deviceName) {
        Optional<User> user = username == null ? Optional.empty() : userRepository.findByUsername(username);
        String hash = user.map(User::getPassword).orElse(dummyHash);
        boolean matches = password != null && passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !matches || !user.get().isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(issue(user.get(), deviceName));
    }

    @Transactional
    public IssuedToken issue(User user, String deviceName) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime now = now();
        apiTokenRepository.deleteExpired(now); // housekeeping, so expired rows don't pile up
        ApiToken apiToken = new ApiToken();
        apiToken.setUser(user);
        apiToken.setTokenHash(hash(token));
        apiToken.setDeviceName(deviceName == null ? null : deviceName.substring(0, Math.min(100, deviceName.length())));
        apiToken.setCreatedDttm(now);
        apiToken.setExpiresDttm(now.plus(validity));
        apiTokenRepository.save(apiToken);
        return new IssuedToken(token, apiToken.getExpiresDttm(), user);
    }

    /** The user a token belongs to, if the token exists, hasn't expired and the account is enabled. */
    @Transactional
    public Optional<User> authenticate(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return Optional.empty();
        }
        Optional<ApiToken> found = apiTokenRepository.findByTokenHash(hash(token));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        ApiToken apiToken = found.get();
        LocalDateTime now = now();
        if (apiToken.getExpiresDttm().isBefore(now) || !apiToken.getUser().isEnabled()) {
            return Optional.empty();
        }
        if (apiToken.getLastUsedDttm() == null || apiToken.getLastUsedDttm().isBefore(now.minus(LAST_USED_RESOLUTION))) {
            apiToken.setLastUsedDttm(now);
            apiTokenRepository.save(apiToken);
        }
        return Optional.of(apiToken.getUser());
    }

    @Transactional
    public void revoke(String token) {
        if (token != null) {
            apiTokenRepository.deleteByTokenHash(hash(token));
        }
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
