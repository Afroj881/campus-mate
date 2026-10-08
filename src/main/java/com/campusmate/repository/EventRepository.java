package com.campusmate.repository;

import com.campusmate.model.Event;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByOrderByDateAscTimeAsc();

    List<Event> findByDateGreaterThanEqualOrderByDateAscTimeAsc(LocalDate date);

    @Query("select e from Event e where e.date > :today or (e.date = :today and e.time >= :time) order by e.date asc, e.time asc")
    List<Event> findUpcomingEvents(@Param("today") LocalDate today, @Param("time") java.time.LocalTime time);

        List<Event> findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOrganizerContainingIgnoreCaseOrVenueContainingIgnoreCase(
            String title, String description, String organizer, String venue);

    @Query("select e from Event e where e.date > :today or (e.date = :today and e.time >= :time) order by e.date asc, e.time asc")
    List<Event> findUpcomingForDashboard(@Param("today") LocalDate today,
                                         @Param("time") java.time.LocalTime time,
                                         Pageable pageable);

    @Query("select count(e) from Event e where e.date > :today or (e.date = :today and e.time >= :time)")
    long countUpcomingFrom(@Param("today") LocalDate today, @Param("time") java.time.LocalTime time);
}
