package com.volunteerportal.app.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.volunteerportal.app.model.ConfigSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConfigSetRepositoryTest {

    @Autowired
    private ConfigSetRepository configSetRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByConfigsetKey_returnsMatchingEntry() {
        persistConfig("test.key.one_ut", "value-one");

        assertThat(configSetRepository.findByConfigsetKey("test.key.one_ut"))
                .get()
                .extracting(ConfigSet::getConfigsetValue)
                .isEqualTo("value-one");
    }

    @Test
    void existsByConfigsetKeyAndIdNot_excludesGivenId() {
        ConfigSet saved = persistConfig("test.key.two_ut", "value-two");

        assertThat(configSetRepository.existsByConfigsetKeyAndIdNot("test.key.two_ut", saved.getId())).isFalse();
        assertThat(configSetRepository.existsByConfigsetKeyAndIdNot("test.key.two_ut", saved.getId() + 1)).isTrue();
        assertThat(configSetRepository.existsByConfigsetKey("test.key.two_ut")).isTrue();
        assertThat(configSetRepository.existsByConfigsetKey("no.such.key_ut")).isFalse();
    }

    private ConfigSet persistConfig(String key, String value) {
        ConfigSet configSet = new ConfigSet();
        configSet.setConfigsetKey(key);
        configSet.setConfigsetValue(value);
        return entityManager.persistFlushFind(configSet);
    }
}
