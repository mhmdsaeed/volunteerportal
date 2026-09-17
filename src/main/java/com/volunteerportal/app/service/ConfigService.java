package com.volunteerportal.app.service;

import org.springframework.stereotype.Service;

import com.volunteerportal.app.repository.ConfigSetRepository;

@Service
public class ConfigService {

    private final ConfigSetRepository configSetRepository;

    public ConfigService(ConfigSetRepository configSetRepository) {
        this.configSetRepository = configSetRepository;
    }

    public String getValue(String key, String defaultValue) {
        return configSetRepository.findByConfigsetKey(key)
                .map(config -> config.getConfigsetValue())
                .orElse(defaultValue);
    }
}
