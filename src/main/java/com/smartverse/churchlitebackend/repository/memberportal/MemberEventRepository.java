package com.smartverse.churchlitebackend.repository.memberportal;

import com.smartverse.churchlitebackend_gen.entities.AppointmentsEntity;
import com.smartverse.churchlitebackend_gen.enums.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MemberEventRepository extends JpaRepository<AppointmentsEntity, UUID> {
    @EntityGraph(attributePaths = "eventsType")
    @Query("select a from AppointmentsEntity a where a.status = :status and a.initialDate >= :now order by a.initialDate")
    List<AppointmentsEntity> upcomingEvents(@Param("status") AppointmentStatus status,
                                            @Param("now") LocalDateTime now,
                                            Pageable pageable);
}
