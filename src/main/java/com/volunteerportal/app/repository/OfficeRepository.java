package com.volunteerportal.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.Office;

public interface OfficeRepository extends JpaRepository<Office, Long> {

    /**
     * Overridden to eagerly fetch role/user: office_role_id and office_user_id are lazy
     * associations, and the list view renders their name/username after the request's
     * Hibernate session has closed (open-in-view is disabled), which would otherwise throw
     * LazyInitializationException.
     */
    @Override
    @EntityGraph(attributePaths = {"role", "user"})
    List<Office> findAll();
}
