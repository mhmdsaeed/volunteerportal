package com.volunteerportal.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.volunteerportal.app.model.ApiToken;
import com.volunteerportal.app.model.User;
import com.volunteerportal.app.repository.ApiTokenRepository;
import com.volunteerportal.app.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    @Mock
    private ApiTokenRepository apiTokenRepository;

    @Mock
    private UserRepository userRepository;

    // A real (fast) encoder so passwords are really checked
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private ApiTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ApiTokenService(apiTokenRepository, userRepository, passwordEncoder, Duration.ofDays(30),
                Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        user.setId(1L);
        user.setUsername("vol");
        user.setPassword(passwordEncoder.encode("right-password"));
        user.setEnabled(true);
        lenient().when(userRepository.findByUsername("vol")).thenReturn(Optional.of(user));
        lenient().when(apiTokenRepository.save(any(ApiToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void login_rightPassword_issuesATokenAndStoresOnlyItsHash() {
        Optional<ApiTokenService.IssuedToken> issued = service.login("vol", "right-password", "Pixel 8");

        assertThat(issued).isPresent();
        String token = issued.get().token();
        assertThat(token).startsWith("vp_").hasSizeGreaterThan(40);
        assertThat(issued.get().expiresAt()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneId.of("UTC")).plusDays(30));

        ArgumentCaptor<ApiToken> saved = ArgumentCaptor.forClass(ApiToken.class);
        verify(apiTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(ApiTokenService.hash(token)).doesNotContain(token);
        assertThat(saved.getValue().getDeviceName()).isEqualTo("Pixel 8");
        assertThat(saved.getValue().getUser()).isSameAs(user);
    }

    @Test
    void login_eachTimeGivesADifferentToken() {
        String first = service.login("vol", "right-password", null).orElseThrow().token();
        String second = service.login("vol", "right-password", null).orElseThrow().token();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void login_wrongPasswordUnknownUserOrDisabledAccount_issuesNothing() {
        given(userRepository.findByUsername("nobody")).willReturn(Optional.empty());

        assertThat(service.login("vol", "wrong", null)).isEmpty();
        assertThat(service.login("nobody", "right-password", null)).isEmpty();
        assertThat(service.login(null, null, null)).isEmpty();

        user.setEnabled(false);
        assertThat(service.login("vol", "right-password", null)).isEmpty();

        verify(apiTokenRepository, never()).save(any());
    }

    @Test
    void authenticate_validToken_returnsTheUser_andRecordsLastUse() {
        ApiToken stored = stored(NOW.plusSeconds(3600), null);

        assertThat(service.authenticate("vp_abc")).contains(user);
        assertThat(stored.getLastUsedDttm()).isNotNull();
        verify(apiTokenRepository).save(stored);
    }

    @Test
    void authenticate_recentlyUsedToken_doesNotWriteAgain() {
        stored(NOW.plusSeconds(3600), LocalDateTime.ofInstant(NOW.minusSeconds(60), ZoneId.of("UTC")));

        assertThat(service.authenticate("vp_abc")).contains(user);
        verify(apiTokenRepository, never()).save(any());
    }

    @Test
    void authenticate_expiredUnknownMalformedOrDisabled_isRejected() {
        stored(NOW.minusSeconds(1), null);
        assertThat(service.authenticate("vp_abc")).isEmpty();

        given(apiTokenRepository.findByTokenHash(ApiTokenService.hash("vp_other"))).willReturn(Optional.empty());
        assertThat(service.authenticate("vp_other")).isEmpty();

        assertThat(service.authenticate("no-prefix")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();

        stored(NOW.plusSeconds(3600), null);
        user.setEnabled(false);
        assertThat(service.authenticate("vp_abc")).isEmpty();
    }

    @Test
    void revoke_deletesByHash() {
        service.revoke("vp_abc");

        verify(apiTokenRepository).deleteByTokenHash(ApiTokenService.hash("vp_abc"));
    }

    @Test
    void hash_isStableSha256Hex() {
        assertThat(ApiTokenService.hash("vp_abc")).hasSize(64).matches("[0-9a-f]+").isEqualTo(ApiTokenService.hash("vp_abc"));
        assertThat(ApiTokenService.hash("vp_abd")).isNotEqualTo(ApiTokenService.hash("vp_abc"));
    }

    private ApiToken stored(Instant expires, LocalDateTime lastUsed) {
        ApiToken token = new ApiToken();
        token.setUser(user);
        token.setExpiresDttm(LocalDateTime.ofInstant(expires, ZoneId.of("UTC")));
        token.setLastUsedDttm(lastUsed);
        given(apiTokenRepository.findByTokenHash(ApiTokenService.hash("vp_abc"))).willReturn(Optional.of(token));
        return token;
    }
}
