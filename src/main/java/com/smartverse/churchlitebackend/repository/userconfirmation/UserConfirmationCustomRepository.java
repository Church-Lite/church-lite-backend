package com.smartverse.churchlitebackend.repository.userconfirmation;

import com.smartverse.churchlitebackend_gen.UserConfirmationEntity;
import com.smartverse.churchlitebackend_gen.UserConfirmationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConfirmationCustomRepository extends UserConfirmationRepository {
    Optional<UserConfirmationEntity> findByHash(String hash);
}
