package com.smartverse.churchlitebackend.repository.userconfirmation;


import com.smartverse.churchlitebackend_gen.entities.UserConfirmationEntity;
import com.smartverse.churchlitebackend_gen.repositories.UserConfirmationRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public interface UserConfirmationCustomRepository extends UserConfirmationRepository {
    Optional<UserConfirmationEntity> findByHash(String hash);
}
