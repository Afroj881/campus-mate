package com.campusmate.service;

import com.campusmate.model.Club;
import com.campusmate.model.Event;
import com.campusmate.model.Notice;
import com.campusmate.model.Resource;
import com.campusmate.model.SearchResult;
import com.campusmate.repository.ClubRepository;
import com.campusmate.repository.EventRepository;
import com.campusmate.repository.NoticeRepository;
import com.campusmate.repository.ResourceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final int DESCRIPTION_LIMIT = 180;

    private final NoticeRepository noticeRepository;
    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final ResourceRepository resourceRepository;

    public SearchService(NoticeRepository noticeRepository,
                         EventRepository eventRepository,
                         ClubRepository clubRepository,
                         ResourceRepository resourceRepository) {
        this.noticeRepository = noticeRepository;
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
        this.resourceRepository = resourceRepository;
    }

    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String searchTerm = query.trim();
        List<SearchResult> results = new ArrayList<>();

        for (Notice notice : noticeRepository
                .findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        searchTerm, searchTerm, searchTerm)) {
            results.add(new SearchResult("Notice", notice.getTitle(), excerpt(notice.getDescription()),
                    notice.getCategory(), "/notices", null));
        }

        for (Event event : eventRepository
                .findAllByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrOrganizerContainingIgnoreCaseOrVenueContainingIgnoreCase(
                        searchTerm, searchTerm, searchTerm, searchTerm)) {
            results.add(new SearchResult("Event", event.getTitle(), excerpt(event.getDescription()),
                    event.getOrganizer() + " · " + event.getVenue(), "/events", null));
        }

        for (Club club : clubRepository
                .findAllByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrFacultyCoordinatorContainingIgnoreCaseOrStudentCoordinatorContainingIgnoreCase(
                        searchTerm, searchTerm, searchTerm, searchTerm)) {
            String coordinators = "Faculty: " + club.getFacultyCoordinator()
                    + " · Student: " + club.getStudentCoordinator();
            results.add(new SearchResult("Club", club.getName(), excerpt(club.getDescription()),
                    coordinators, "/clubs", null));
        }

        for (Resource resource : resourceRepository.searchUrlResources(searchTerm)) {
            results.add(new SearchResult("Resource", resource.getTitle(), excerpt(resource.getDescription()),
                    resource.getSubject(), "/resources", resource.getUrl()));
        }

        return results;
    }

    private String excerpt(String description) {
        if (description == null || description.length() <= DESCRIPTION_LIMIT) {
            return description;
        }
        return description.substring(0, DESCRIPTION_LIMIT - 3).stripTrailing() + "...";
    }
}
