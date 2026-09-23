package com.volunteerportal.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.volunteerportal.app.model.ApiToken;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    /**
     * With the user and their roles: the request is authenticated from them after the transaction
     * ends. (roles must be listed - a fetch graph makes everything it doesn't name lazy, even EAGER.)
     */
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<ApiToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from ApiToken t where t.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from ApiToken t where t.expiresDttm < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    long countByUserId(Long userId);
}
