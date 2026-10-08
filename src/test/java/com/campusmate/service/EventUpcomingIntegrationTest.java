package com.campusmate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campusmate.model.Event;
import com.campusmate.repository.EventRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class EventUpcomingIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    @Transactional
    void upcomingEventsExcludeEarlierTimesOnTheCurrentDate() {
        String suffix = UUID.randomUUID().toString();
        LocalDate today = LocalDate.of(2030, 5, 12);
        Event earlierToday = event("Earlier " + suffix, today, LocalTime.of(9, 0));
        Event laterToday = event("Later " + suffix, today, LocalTime.of(13, 0));
        Event tomorrow = event("Tomorrow " + suffix, today.plusDays(1), LocalTime.MIDNIGHT);
        eventRepository.saveAll(List.of(earlierToday, laterToday, tomorrow));

        List<String> upcomingTitles = eventRepository.findUpcomingEvents(today, LocalTime.NOON).stream()
                .filter(event -> event.getTitle().endsWith(suffix))
                .map(Event::getTitle)
                .toList();

        assertEquals(List.of(laterToday.getTitle(), tomorrow.getTitle()), upcomingTitles);
    }

    private Event event(String title, LocalDate date, LocalTime time) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription("Event timing query test.");
        event.setDate(date);
        event.setTime(time);
        event.setVenue("Campus");
        event.setOrganizer("College");
        return event;
    }
}
