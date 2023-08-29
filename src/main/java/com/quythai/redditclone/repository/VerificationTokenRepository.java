package com.quythai.redditclone.repository;

import com.quythai.redditclone.model.VerificatitonToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificatitonToken, Long> {
    Optional<VerificatitonToken> findByToken(String token);
}
