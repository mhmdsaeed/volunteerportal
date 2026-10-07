package com.volunteerportal.app.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.volunteerportal.app.model.PasswordResetToken;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.PasswordResetTokenRepository;
import com.volunteerportal.app.repository.UserRepository;

@Service
public class PasswordServiceImpl implements PasswordService {

    private static final int TOKEN_BYTES = 32;
    /** A second "Forgot your password?" for the same account within this time sends nothing. */
    static final Duration RESEND_AFTER = Duration.ofMinutes(2);

    private final UserRepository userRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer mailer;
    private final Duration validity;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PasswordServiceImpl(UserRepository userRepository, ApiTokenRepository apiTokenRepository,
            PasswordResetTokenRepository resetTokenRepository, PasswordEncoder passwordEncoder,
            PasswordResetMailer mailer, @Value("${app.password-reset.validity:60m}") Duration validity) {
        this(userRepository, apiTokenRepository, resetTokenRepository, passwordEncoder, mailer, validity,
                Clock.systemDefaultZone());
    }

    PasswordServiceImpl(UserRepository userRepository, ApiTokenRepository apiTokenRepository,
            PasswordResetTokenRepository resetTokenRepository, PasswordEncoder passwordEncoder,
            PasswordResetMailer mailer, Duration validity, Clock clock) {
        this.userRepository = userRepository;
        this.apiTokenRepository = apiTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.validity = validity;
        this.clock = clock;
    }

    @Override
    @Transactional
    public User changeOwnPassword(Long userId, String currentPassword, String newPassword, String keepApiToken) {
        User user = findUser(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new PasswordRejectedException("password.error.wrongCurrent");
        }
        checkLength(newPassword);
        return apply(user, newPassword, keepApiToken);
    }

    @Override
    @Transactional
    public User setPassword(Long userId, String newPassword, Long adminId) {
        if (userId.equals(adminId)) {
            throw new PasswordRejectedException("password.error.ownAccount");
        }
        User user = findUser(userId);
        checkLength(newPassword);
        return apply(user, newPassword, null);
    }

    @Override
    public boolean resetByEmailAvailable() {
        return mailer.available();
    }

    @Override
    @Transactional
    public void requestReset(String email, String linkBase, Locale locale) {
        if (!mailer.available() || !StringUtils.hasText(email)) {
            return;
        }
        Optional<User> found = userRepository.findByEmail(email.trim());
        if (found.isEmpty() || !found.get().isEnabled()) {
            return;
        }
        User user = found.get();
        LocalDateTime now = now();
        resetTokenRepository.deleteExpired(now); // housekeeping, so expired rows don't pile up
        if (resetTokenRepository.existsByUserIdAndCreatedDttmAfter(user.getId(), now.minus(RESEND_AFTER))) {
            return;
        }
        resetTokenRepository.deleteByUserId(user.getId()); // only the newest link works

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(ApiTokenService.hash(token));
        resetToken.setCreatedDttm(now);
        resetToken.setExpiresDttm(now.plus(validity));
        resetTokenRepository.save(resetToken);

        mailer.send(user.getUsername(), user.getEmail(), linkBase + "/reset-password?token=" + token, validity, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findResetUser(String token) {
        return validToken(token).map(PasswordResetToken::getUser);
    }

    @Override
    @Transactional
    public User resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validToken(token)
                .orElseThrow(() -> new PasswordRejectedException("password.error.invalidLink"));
        checkLength(newPassword);
        return apply(resetToken.getUser(), newPassword, null);
    }

    private Optional<PasswordResetToken> validToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        LocalDateTime now = now();
        return resetTokenRepository.findByTokenHash(ApiTokenService.hash(token))
                .filter(t -> t.getExpiresDttm().isAfter(now) && t.getUser().isEnabled());
    }

    /** Saves the new password and signs the user out elsewhere; also voids any reset link they were sent. */
    private User apply(User user, String newPassword, String keepApiToken) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        if (keepApiToken == null) {
            apiTokenRepository.deleteByUserId(user.getId());
        } else {
            apiTokenRepository.deleteByUserIdExcept(user.getId(), ApiTokenService.hash(keepApiToken));
        }
        resetTokenRepository.deleteByUserId(user.getId());
        return user;
    }

    private static void checkLength(String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_LENGTH) {
            throw new PasswordRejectedException("password.error.tooShort");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
