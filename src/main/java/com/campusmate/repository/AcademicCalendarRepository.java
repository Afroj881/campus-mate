package com.campusmate.repository;

import com.campusmate.model.AcademicCalendarEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendarEntry, Long> {
    List<AcademicCalendarEntry> findAllByOrderByEventDateAscTitleAscIdAsc();
}
