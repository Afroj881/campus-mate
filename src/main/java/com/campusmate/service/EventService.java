package com.campusmate.service;

import com.campusmate.model.Event;
import com.campusmate.repository.EventRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event createEvent(Event event) {
        event.setId(null);
        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAllByOrderByDateAscTimeAsc();
    }

    public List<Event> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDate.now(), LocalTime.now());
    }

    public List<Event> getUpcomingDashboardEvents() {
        return eventRepository.findUpcomingForDashboard(LocalDate.now(), LocalTime.now(), PageRequest.of(0, 5));
    }

    public long countUpcomingEvents() {
        return eventRepository.countUpcomingFrom(LocalDate.now(), LocalTime.now());
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    public Event updateEvent(Long id, Event event) {
        Event existingEvent = getEventById(id);
        existingEvent.setTitle(event.getTitle());
        existingEvent.setDescription(event.getDescription());
        existingEvent.setDate(event.getDate());
        existingEvent.setTime(event.getTime());
        existingEvent.setVenue(event.getVenue());
        existingEvent.setOrganizer(event.getOrganizer());
        return eventRepository.save(existingEvent);
    }

    public void deleteEvent(Long id) {
        eventRepository.delete(getEventById(id));
    }

    public long countEvents() {
        return eventRepository.count();
    }
}
