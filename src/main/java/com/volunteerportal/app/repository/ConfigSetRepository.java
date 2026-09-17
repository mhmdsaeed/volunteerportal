package com.volunteerportal.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.volunteerportal.app.model.ConfigSet;

public interface ConfigSetRepository extends JpaRepository<ConfigSet, Long> {

    Optional<ConfigSet> findByConfigsetKey(String configsetKey);

    boolean existsByConfigsetKey(String configsetKey);

    boolean existsByConfigsetKeyAndIdNot(String configsetKey, Long id);
}
