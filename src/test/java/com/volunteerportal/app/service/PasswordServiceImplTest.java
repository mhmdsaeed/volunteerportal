package com.volunteerportal.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.volunteerportal.app.model.PasswordResetToken;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.PasswordResetTokenRepository;
import com.volunteerportal.app.repository.UserRepository;
import com.volunteerportal.app.service.PasswordService.PasswordRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PasswordServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 12, 0);
    private static final Duration VALIDITY = Duration.ofMinutes(60);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApiTokenRepository apiTokenRepository = mock(ApiTokenRepository.class);
    private final PasswordResetTokenRepository resetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordResetMailer mailer = mock(PasswordResetMailer.class);
    // A low cost keeps the test fast; the application uses the default
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private PasswordServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordServiceImpl(userRepository, apiTokenRepository, resetTokenRepository, encoder, mailer,
                VALIDITY, clock);
        user = new User();
        user.setId(7L);
        user.setUsername("sara");
        user.setEmail("sara@example.com");
        user.setEnabled(true);
        user.setPassword(encoder.encode("old-password"));
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(mailer.available()).willReturn(true);
    }

    @Test
    void changeOwnPassword_withTheRightCurrentOne_savesTheNewHash_andSignsOutEverywhereElse() {
        User saved = service.changeOwnPassword(7L, "old-password", "new-password", null);

        assertThat(encoder.matches("new-password", saved.getPassword())).isTrue();
        verify(userRepository).save(user);
        verify(apiTokenRepository).deleteByUserId(7L);
        verify(resetTokenRepository).deleteByUserId(7L);
    }

    @Test
    void changeOwnPassword_fromTheApp_keepsThatAppLogin() {
        service.changeOwnPassword(7L, "old-password", "new-password", "vp_this-phone");

        verify(apiTokenRepository).deleteByUserIdExcept(7L, ApiTokenService.hash("vp_this-phone"));
        verify(apiTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void changeOwnPassword_withAWrongCurrentOne_isRefused() {
        assertThatThrownBy(() -> service.changeOwnPassword(7L, "guess", "new-password", null))
                .isInstanceOf(PasswordRejectedException.class)
                .hasMessage("password.error.wrongCurrent");
        verify(userRepository, never()).save(any());
    }

    @Test
    void aNewPasswordShorterThanEightCharacters_isRefused() {
        assertThatThrownBy(() -> service.changeOwnPassword(7L, "old-password", "short", null))
                .hasMessage("password.error.tooShort");
        assertThatThrownBy(() -> service.setPassword(7L, null, 1L))
                .hasMessage("password.error.tooShort");
        verify(userRepository, never()).save(any());
    }

    @Test
    void anAdminSetsSomeoneElsesPassword_andTheyAreSignedOut() {
        User saved = service.setPassword(7L, "given-by-admin", 1L);

        assertThat(encoder.matches("given-by-admin", saved.getPassword())).isTrue();
        verify(apiTokenRepository).deleteByUserId(7L);
    }

    @Test
    void anAdminCantSetTheirOwnPasswordThatWay() {
        assertThatThrownBy(() -> service.setPassword(7L, "given-by-admin", 7L))
                .hasMessage("password.error.ownAccount");
        verify(userRepository, never()).save(any());
    }

    @Test
    void requestReset_emailsASingleUseLinkThatHoldsOnlyTheTokensHash() {
        given(userRepository.findByEmail("sara@example.com")).willReturn(Optional.of(user));

        service.requestReset(" sara@example.com ", "https://portal.example.org", Locale.ENGLISH);

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("sara"), eq("sara@example.com"), link.capture(), eq(VALIDITY), eq(Locale.ENGLISH));
        assertThat(link.getValue()).startsWith("https://portal.example.org/reset-password?token=");
        String token = link.getValue().substring(link.getValue().indexOf('=') + 1);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(ApiTokenService.hash(token)).isNotEqualTo(token);
        assertThat(saved.getValue().getExpiresDttm()).isEqualTo(NOW.plus(VALIDITY));
        verify(resetTokenRepository).deleteByUserId(7L); // older links stop working
    }

    @Test
    void requestReset_forAnUnknownOrDeactivatedEmail_sendsNothing() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());
        service.requestReset("nobody@example.com", "https://portal.example.org", Locale.ENGLISH);

        user.setEnabled(false);
        given(userRepository.findByEmail("sara@example.com")).willReturn(Optional.of(user));
        service.requestReset("sara@example.com", "https://portal.example.org", Locale.ENGLISH);

        verify(mailer, never()).send(anyString(), anyString(), anyString(), any(), any());
        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    void requestReset_againWithinTwoMinutes_sendsNothing() {
        given(userRepository.findByEmail("sara@example.com")).willReturn(Optional.of(user));
        given(resetTokenRepository.existsByUserIdAndCreatedDttmAfter(7L, NOW.minus(PasswordServiceImpl.RESEND_AFTER)))
                .willReturn(true);

        service.requestReset("sara@example.com", "https://portal.example.org", Locale.ENGLISH);

        verify(mailer, never()).send(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void requestReset_withoutMail_doesNothing() {
        given(mailer.available()).willReturn(false);

        service.requestReset("sara@example.com", "https://portal.example.org", Locale.ENGLISH);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void resetPassword_withAValidLink_setsThePassword_andUsesTheLinkUp() {
        given(resetTokenRepository.findByTokenHash(ApiTokenService.hash("abc"))).willReturn(Optional.of(token(NOW.plusMinutes(5))));

        User saved = service.resetPassword("abc", "from-the-link");

        assertThat(encoder.matches("from-the-link", saved.getPassword())).isTrue();
        verify(resetTokenRepository).deleteByUserId(7L);
        verify(apiTokenRepository).deleteByUserId(7L);
    }

    @Test
    void anExpiredLink_orOneForADeactivatedAccount_doesNotWork() {
        given(resetTokenRepository.findByTokenHash(ApiTokenService.hash("old"))).willReturn(Optional.of(token(NOW.minusSeconds(1))));
        assertThat(service.findResetUser("old")).isEmpty();
        assertThatThrownBy(() -> service.resetPassword("old", "from-the-link")).hasMessage("password.error.invalidLink");

        user.setEnabled(false);
        given(resetTokenRepository.findByTokenHash(ApiTokenService.hash("abc"))).willReturn(Optional.of(token(NOW.plusMinutes(5))));
        assertThat(service.findResetUser("abc")).isEmpty();

        assertThat(service.findResetUser(null)).isEmpty();
        verify(userRepository, never()).save(any());
    }

    private PasswordResetToken token(LocalDateTime expires) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCreatedDttm(NOW.minusMinutes(1));
        token.setExpiresDttm(expires);
        return token;
    }
}
