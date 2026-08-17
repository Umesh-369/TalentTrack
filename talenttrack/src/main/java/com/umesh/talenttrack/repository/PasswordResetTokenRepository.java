package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.PasswordResetToken;
import com.umesh.talenttrack.domain.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    List<PasswordResetToken> findAllByEmailAndUserTypeAndUsedFalse(String email, UserType userType);
}
