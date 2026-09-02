package com.smartverse.churchlitebackend.repository.cash;

import com.smartverse.churchlitebackend_gen.entities.CashEntity;
import com.smartverse.churchlitebackend_gen.repositories.CashRepository;
import jakarta.persistence.LockModeType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Primary
@Repository
public interface CashCustomRepository extends CashRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cash from CashEntity cash where cash.id in :ids order by cash.id")
    List<CashEntity> findAllForUpdate(@Param("ids") Collection<UUID> ids);
}
