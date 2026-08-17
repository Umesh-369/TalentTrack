package com.umesh.talenttrack.repository;

import com.umesh.talenttrack.domain.RefreshToken;
import com.umesh.talenttrack.domain.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUserIdAndUserType(Long userId, UserType userType);
}
