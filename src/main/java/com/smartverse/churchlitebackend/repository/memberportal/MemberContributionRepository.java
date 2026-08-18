package com.smartverse.churchlitebackend.repository.memberportal;

import com.smartverse.churchlitebackend_gen.entities.FinancialEntity;
import com.smartverse.churchlitebackend_gen.enums.TypeFinancial;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MemberContributionRepository extends JpaRepository<FinancialEntity, UUID> {
    @EntityGraph(attributePaths = "planAccount")
    @Query("select f from FinancialEntity f where f.person.id = :personId and f.typeFinancial = :type " +
           "and f.paymentReceiptDate is not null order by f.paymentReceiptDate desc, f.id desc")
    List<FinancialEntity> recentContributions(@Param("personId") UUID personId,
                                               @Param("type") TypeFinancial type,
                                               Pageable pageable);

    @Query("select coalesce(sum(f.value), 0) from FinancialEntity f where f.person.id = :personId " +
           "and f.typeFinancial = :type and f.paymentReceiptDate between :start and :end")
    Double contributionTotal(@Param("personId") UUID personId,
                             @Param("type") TypeFinancial type,
                             @Param("start") LocalDate start,
                             @Param("end") LocalDate end);
}
