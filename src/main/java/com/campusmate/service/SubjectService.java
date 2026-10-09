package com.campusmate.service;

import com.campusmate.model.Subject;
import com.campusmate.repository.SubjectRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Subject> getSubjectById(Long id) {
        return subjectRepository.findById(id);
    }

    @Transactional
    public Subject createSubject(Subject subject) {
        String code = normalizeCode(subject.getCode());
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Subject code is required.");
        }
        if (subjectRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new DuplicateSubjectException();
        }
        subject.setCode(code);
        subject.setName(subject.getName() == null ? null : subject.getName().trim());
        subject.setDepartment(subject.getDepartment() == null ? null : subject.getDepartment().trim());
        try {
            return subjectRepository.save(subject);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSubjectException();
        }
    }

    @Transactional
    public Subject updateSubject(Long id, Subject updated) {
        Subject existing = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found."));
        String code = normalizeCode(updated.getCode());
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Subject code is required.");
        }
        existing.setName(updated.getName() == null ? null : updated.getName().trim());
        existing.setCode(code);
        existing.setDepartment(updated.getDepartment() == null ? null : updated.getDepartment().trim());
        existing.setSemester(updated.getSemester());
        return subjectRepository.save(existing);
    }

    @Transactional
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new IllegalArgumentException("Subject not found.");
        }
        subjectRepository.deleteById(id);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    public static class DuplicateSubjectException extends RuntimeException {
    }
}
