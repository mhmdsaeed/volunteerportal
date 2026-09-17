package com.volunteerportal.app.service;

import java.util.List;

import com.volunteerportal.app.dto.OfficeForm;
import com.volunteerportal.app.model.Office;

public interface OfficeService {

    List<Office> findAll();

    Office findById(Long id);

    Office create(OfficeForm form);

    Office update(Long id, OfficeForm form);

    void delete(Long id);
}
