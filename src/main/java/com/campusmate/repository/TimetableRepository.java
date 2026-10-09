package com.campusmate.repository;

import com.campusmate.model.Timetable;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    List<Timetable> findAllByDayOrderByStartTimeAsc(DayOfWeek day);
}