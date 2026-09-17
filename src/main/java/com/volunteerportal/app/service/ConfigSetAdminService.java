package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.ConfigSetForm;
import com.volunteerportal.app.model.ConfigSet;

public interface ConfigSetAdminService {

    List<ConfigSet> findAll();

    ConfigSet findById(Long id);

    boolean keyTaken(String key, Long excludeId);

    ConfigSet create(ConfigSetForm form);

    ConfigSet update(Long id, ConfigSetForm form);

    void delete(Long id);
}
