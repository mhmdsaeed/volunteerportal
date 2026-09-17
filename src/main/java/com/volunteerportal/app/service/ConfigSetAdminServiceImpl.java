package com.volunteerportal.app.service;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.dto.ConfigSetForm;
import com.volunteerportal.app.model.ConfigSet;
import com.volunteerportal.app.repository.ConfigSetRepository;

@Service
public class ConfigSetAdminServiceImpl implements ConfigSetAdminService {

    private final ConfigSetRepository configSetRepository;

    public ConfigSetAdminServiceImpl(ConfigSetRepository configSetRepository) {
        this.configSetRepository = configSetRepository;
    }

    @Override
    public List<ConfigSet> findAll() {
        return configSetRepository.findAll();
    }

    @Override
    public ConfigSet findById(Long id) {
        return configSetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Config entry not found: " + id));
    }

    @Override
    public boolean keyTaken(String key, Long excludeId) {
        return excludeId == null ? configSetRepository.existsByConfigsetKey(key)
                : configSetRepository.existsByConfigsetKeyAndIdNot(key, excludeId);
    }

    @Override
    @Transactional
    public ConfigSet create(ConfigSetForm form) {
        ConfigSet configSet = new ConfigSet();
        applyForm(configSet, form);
        return configSetRepository.save(configSet);
    }

    @Override
    @Transactional
    public ConfigSet update(Long id, ConfigSetForm form) {
        ConfigSet configSet = findById(id);
        applyForm(configSet, form);
        return configSetRepository.save(configSet);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        configSetRepository.deleteById(id);
    }

    private void applyForm(ConfigSet configSet, ConfigSetForm form) {
        configSet.setConfigsetKey(form.getConfigsetKey());
        configSet.setConfigsetValue(form.getConfigsetValue());
    }
}
