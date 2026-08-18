package com.smartverse.churchlitebackend.config.security.repository;

import com.smartverse.churchlitebackend.config.security.model.UserSupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuthenticationRepository extends JpaRepository<UserSupplierEntity, UUID>  {

    Optional<UserSupplierEntity> findOneByEmail(String email);

    Optional<UserSupplierEntity> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    Optional<UserSupplierEntity> findFirstByEmailIgnoreCaseAndTenant(String email, String tenant);

    List<UserSupplierEntity> findAllByEmailOrderByIdAsc(String email);

    List<UserSupplierEntity> findAllByCpfOrderByIdAsc(String cpf);

    boolean existsByEmail(String email);

    boolean existsByEmailAndTenant(String email, String tenant);

    boolean existsByEmailAndTenantAndIdNot(String email, String tenant, UUID id);

    int countAllBy();
}
