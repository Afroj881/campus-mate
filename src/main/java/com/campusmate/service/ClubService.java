package com.campusmate.service;

import com.campusmate.model.Club;
import com.campusmate.repository.ClubRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClubService {

    private final ClubRepository clubRepository;

    public ClubService(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    public Club createClub(Club club) {
        club.setId(null);
        return clubRepository.save(club);
    }

    public List<Club> getAllClubs() {
        return clubRepository.findAllByOrderByNameAsc();
    }

    public long countClubs() {
        return clubRepository.count();
    }

    public Club getClubById(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found"));
    }

    public Club updateClub(Long id, Club submittedClub) {
        Club club = getClubById(id);
        club.setName(submittedClub.getName());
        club.setDescription(submittedClub.getDescription());
        club.setFacultyCoordinator(submittedClub.getFacultyCoordinator());
        club.setStudentCoordinator(submittedClub.getStudentCoordinator());
        club.setContact(submittedClub.getContact());
        return clubRepository.save(club);
    }

    public void deleteClub(Long id) {
        clubRepository.delete(getClubById(id));
    }
}
