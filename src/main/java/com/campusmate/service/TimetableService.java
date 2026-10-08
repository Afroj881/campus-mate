package com.campusmate.service;

import com.campusmate.model.Timetable;
import com.campusmate.repository.TimetableRepository;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TimetableService {

    private final TimetableRepository timetableRepository;

    public TimetableService(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    public Timetable createTimetableEntry(Timetable timetable) {
        timetable.setId(null);
        return timetableRepository.save(timetable);
    }

    public List<Timetable> getAllTimetableEntries() {
        return timetableRepository.findAll().stream()
                .sorted(Comparator.comparing(Timetable::getDay).thenComparing(Timetable::getStartTime))
                .toList();
    }

    public List<Timetable> getTimetableEntriesForDay(DayOfWeek day) {
        return timetableRepository.findAllByDayOrderByStartTimeAsc(day);
    }

    public Timetable getTimetableEntryById(Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timetable entry not found"));
    }

    public Timetable updateTimetableEntry(Long id, Timetable timetable) {
        Timetable existingEntry = getTimetableEntryById(id);
        existingEntry.setDay(timetable.getDay());
        existingEntry.setSubject(timetable.getSubject());
        existingEntry.setFaculty(timetable.getFaculty());
        existingEntry.setRoom(timetable.getRoom());
        existingEntry.setStartTime(timetable.getStartTime());
        existingEntry.setEndTime(timetable.getEndTime());
        return timetableRepository.save(existingEntry);
    }

    public void deleteTimetableEntry(Long id) {
        timetableRepository.delete(getTimetableEntryById(id));
    }
}