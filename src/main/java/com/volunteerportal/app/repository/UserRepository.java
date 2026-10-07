package com.volunteerportal.app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.volunteerportal.app.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByRoles_Name(String roleName);

    /**
     * The password hash of an active account, empty if it is deactivated or gone (checked on every website
     * request, so a session ends once the password changes elsewhere; see StaleSessionFilter).
     */
    @Query("select u.password from User u where u.id = :id and u.enabled = true")
    Optional<String> findActivePasswordHash(@Param("id") Long id);

    /** Active users having any of the given roles, by username (e.g. who may supervise an initiative). */
    @Query("select distinct u from User u join u.roles r where r.name in :roleNames and u.enabled = true order by u.username")
    List<User> findActiveWithAnyRole(@Param("roleNames") Collection<String> roleNames);
}
