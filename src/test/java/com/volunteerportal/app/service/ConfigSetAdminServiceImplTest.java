package com.volunteerportal.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.volunteerportal.app.repository.ConfigSetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfigSetAdminServiceImplTest {

    @Mock
    private ConfigSetRepository configSetRepository;

    @InjectMocks
    private ConfigSetAdminServiceImpl service;

    @Test
    void keyTaken_forNewEntry_checksExistsByKey() {
        given(configSetRepository.existsByConfigsetKey("app.setting")).willReturn(true);

        assertThat(service.keyTaken("app.setting", null)).isTrue();
        verify(configSetRepository, never()).existsByConfigsetKeyAndIdNot(any(), any());
    }

    @Test
    void keyTaken_forExistingEntry_excludesItsOwnId() {
        given(configSetRepository.existsByConfigsetKeyAndIdNot("app.setting", 3L)).willReturn(false);

        assertThat(service.keyTaken("app.setting", 3L)).isFalse();
        verify(configSetRepository, never()).existsByConfigsetKey(any());
    }
}
