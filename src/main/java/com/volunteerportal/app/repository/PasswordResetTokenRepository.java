package com.volunteerportal.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.volunteerportal.app.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Whether the user was sent a link after the given time (to avoid flooding their inbox). */
    boolean existsByUserIdAndCreatedDttmAfter(Long userId, LocalDateTime after);

    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.expiresDttm < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
