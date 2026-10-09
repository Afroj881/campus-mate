package com.campusmate.service;

import com.campusmate.model.AcademicCalendarEntry;
import com.campusmate.repository.AcademicCalendarRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AcademicCalendarService {

    private final AcademicCalendarRepository repository;

    public AcademicCalendarService(AcademicCalendarRepository repository) {
        this.repository = repository;
    }

    public List<AcademicCalendarEntry> getAllEntries() {
        return repository.findAllByOrderByEventDateAscTitleAscIdAsc();
    }

    public AcademicCalendarEntry getEntry(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar entry not found."));
    }

    public AcademicCalendarEntry createEntry(AcademicCalendarEntry entry) {
        entry.setId(null);
        return repository.save(entry);
    }

    public AcademicCalendarEntry updateEntry(Long id, AcademicCalendarEntry submitted) {
        AcademicCalendarEntry entry = getEntry(id);
        entry.setTitle(submitted.getTitle());
        entry.setDescription(submitted.getDescription());
        entry.setEventDate(submitted.getEventDate());
        entry.setEndDate(submitted.getEndDate());
        entry.setEventType(submitted.getEventType());
        return repository.save(entry);
    }

    public void deleteEntry(Long id) {
        repository.delete(getEntry(id));
    }
}
