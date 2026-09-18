package com.volunteerportal.app.service;

import com.volunteerportal.app.model.Initiative;

public interface InitiativeDuplicateService {

    /**
     * Creates a disabled copy of the initiative (name suffixed "(Copy)"), along with copies of
     * all its questions. Join requests, events, and attendance are not carried over.
     */
    Initiative duplicate(Long initiativeId);
}
