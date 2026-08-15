package com.smartverse.churchlitebackend.repository.report;

import com.smartverse.churchlitebackend_gen.entities.ScreenReportEntity;
import com.smartverse.churchlitebackend_gen.repositories.ScreenReportRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;

@Primary
@Repository
public interface ScreenReportCustomRepository extends ScreenReportRepository {
    List<ScreenReportEntity> findAllByScreenAndActiveTrueOrderByDisplayOrderAscNameAsc(String screen);
}
