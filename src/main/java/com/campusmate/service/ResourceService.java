package com.campusmate.service;

import com.campusmate.model.Resource;
import com.campusmate.repository.ResourceRepository;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.campusmate.model.Subject;
import com.campusmate.model.TeacherSubjectAssignment;
import com.campusmate.model.User;
import com.campusmate.repository.TeacherSubjectAssignmentRepository;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final TeacherSubjectAssignmentRepository assignmentRepository;
    private final Path uploadDirectory;
    private final long maxFileSize;

    public ResourceService(ResourceRepository resourceRepository, TeacherSubjectAssignmentRepository assignmentRepository,
                           @Value("${app.upload.dir:uploads/resources}") String uploadDirectory,
                           @Value("${app.upload.max-file-size:10485760}") long maxFileSize) {
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    public Resource createResource(Resource resource) {
        if (resource.getUrl() == null || !resource.getUrl().matches("(?i)^https?://\\S+$")) {
            throw new IllegalArgumentException("Enter a valid HTTP or HTTPS URL.");
        }
        resource.setId(null);
        return resourceRepository.save(resource);
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAllByUploadedByIsNullOrderBySubjectAscTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<Resource> getStudentUrlResources(User student) {
        if (student == null || student.getRole() != User.Role.STUDENT
                || student.getDepartment() == null || student.getDepartment().isBlank()
                || student.getSemester() == null || student.getSection() == null || student.getSection().isBlank()) {
            return List.of();
        }
        return resourceRepository.findUrlResourcesForClass(
                student.getDepartment().trim(), student.getSemester(), student.getSection().trim());
    }

    @Transactional(readOnly = true)
    public List<Resource> getFacultyFiles(User faculty) { return resourceRepository.findFilesByUploadedBy(faculty); }

    @Transactional(readOnly = true)
    public List<Resource> getStudentFiles(User student) {
        if (student.getDepartment() == null || student.getSemester() == null || student.getSection() == null) return List.of();
        return resourceRepository.findFilesForClass(student.getDepartment(), student.getSemester(), student.getSection());
    }

    @Transactional
    public Resource uploadFile(User faculty, Long subjectId, Integer semester, String section,
                               String title, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a non-empty notes file.");
        if (file.getSize() > maxFileSize) throw new IllegalArgumentException("File exceeds the configured upload limit.");
        Subject subject = assignmentRepository.findByFacultyOrderBySemesterAscSubjectNameAsc(faculty).stream()
                .filter(a -> a.getSubject().getId().equals(subjectId) && a.getSemester().equals(semester)
                        && a.getSection().equalsIgnoreCase(section == null ? "" : section.trim()))
                .map(TeacherSubjectAssignment::getSubject).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("You are not assigned to that subject and class."));
        TeacherSubjectAssignment assignment = assignmentRepository.findByFacultyAndSubjectAndSemesterAndSection(
                faculty, subject, semester, section.trim()).orElseThrow(() -> new IllegalArgumentException("You are not assigned to that subject and class."));
        String original = sanitizeFilename(file.getOriginalFilename());
        String extension = extension(original);
        String contentType = supportedContentType(extension, file);
        String stored = UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory.resolve(stored).normalize();
        if (!destination.startsWith(uploadDirectory)) throw new IllegalArgumentException("Invalid file name.");
        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), destination);
        } catch (IOException ex) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw new IllegalArgumentException("The notes file could not be stored.");
        }
        Resource resource = new Resource();
        resource.setTitle(title == null ? "" : title.trim());
        resource.setSubject(subject.getName());
        resource.setDescription(description == null ? "" : description.trim());
        // The original URL column is non-null in existing databases; keep its schema and link records intact.
        resource.setUrl("");
        resource.setOriginalFilename(original);
        resource.setStoredFilename(stored);
        resource.setContentType(contentType);
        resource.setFileSize(file.getSize());
        resource.setUploadedBy(faculty);
        resource.setDepartment(assignment.getDepartment());
        resource.setSemester(assignment.getSemester());
        resource.setSection(assignment.getSection());
        resource.setCreatedAt(LocalDateTime.now());
        try {
            Resource saved = resourceRepository.save(resource);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) deleteStoredFile(stored);
                    }
                });
            }
            return saved;
        } catch (RuntimeException ex) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw ex;
        }
    }

    @Transactional
    public Resource uploadFileForAssignment(User faculty, Long assignmentId, String title, String description, MultipartFile file) {
        TeacherSubjectAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Select one of your assigned subjects and classes."));
        if (assignment.getFaculty() == null || !assignment.getFaculty().getId().equals(faculty.getId()))
            throw new IllegalArgumentException("You are not assigned to that subject and class.");
        return uploadFile(faculty, assignment.getSubject().getId(), assignment.getSemester(), assignment.getSection(),
                title, description, file);
    }

    @Transactional
    public Resource uploadFileForAssignmentLabel(User faculty, String subjectAndClass, String title, String description, MultipartFile file) {
        String submitted = subjectAndClass == null ? "" : subjectAndClass.trim();
        if (submitted.isBlank()) throw new IllegalArgumentException("Subject and class is required.");
        TeacherSubjectAssignment assignment = assignmentRepository.findByFacultyIdWithSubject(faculty.getId()).stream()
                .filter(candidate -> normalizeAssignmentLabel(assignmentLabel(candidate)).equals(normalizeAssignmentLabel(submitted)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("You can upload notes only for your assigned teaching classes."));
        return uploadFileForAssignment(faculty, assignment.getId(), title, description, file);
    }

    private String assignmentLabel(TeacherSubjectAssignment assignment) {
        return assignment.getSubject().getName() + " \u2014 " + assignment.getDepartment() + " \u2014 Semester "
                + assignment.getSemester() + " \u2014 Section " + assignment.getSection();
    }

    private String normalizeAssignmentLabel(String value) {
        // Accept common dash variants and the legacy UTF-8-as-Windows-1252 separator encoding.
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("\\u00e2\\u20ac[\\u2010-\\u2015]", "|")
                .replaceAll("[\\u2010-\\u2015\\u2212\\ufe58\\ufe63\\uff0d-]", "|")
                .replaceAll("\\s*\\|\\s*", "|")
                .replaceAll("\\s+", " ")
                .trim();
    }
    @Transactional(readOnly = true)
    public Resource getAuthorizedFile(Long id, User user) {
        Resource resource = resourceRepository.findWithUploaderById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found."));
        if (!resource.isFileBacked()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found.");
        if (user.getRole() == User.Role.FACULTY && resource.getUploadedBy().getId().equals(user.getId())) return resource;
        if (user.getRole() == User.Role.STUDENT && sameClass(resource, user)) return resource;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this resource.");
    }

    @Transactional
    public void deleteFacultyFile(Long id, User faculty) {
        Resource resource = resourceRepository.findOwnedFile(id, faculty)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found."));
        String stored = resource.getStoredFilename();
        resourceRepository.delete(resource);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteStoredFile(stored); }
            });
        }
    }

    public Path resolveStoredFile(String filename) {
        try { return safePath(filename); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource file not found."); }
    }

    private Path safePath(String filename) {
        if (filename == null || filename.contains("/") || filename.contains("\\") || filename.contains("..")) throw new IllegalArgumentException();
        Path path = uploadDirectory.resolve(filename).normalize();
        if (!path.startsWith(uploadDirectory)) throw new IllegalArgumentException();
        return path;
    }

    private void deleteStoredFile(String filename) {
        try { Files.deleteIfExists(safePath(filename)); } catch (IOException | IllegalArgumentException ignored) { }
    }

    private boolean sameClass(Resource r, User u) {
        return u.getRole() == User.Role.STUDENT && sameScopeValue(r.getDepartment(), u.getDepartment())
                && r.getSemester() != null && r.getSemester().equals(u.getSemester())
                && sameScopeValue(r.getSection(), u.getSection());
    }

    private boolean sameScopeValue(String resourceValue, String userValue) {
        return resourceValue != null && userValue != null
                && resourceValue.trim().equalsIgnoreCase(userValue.trim());
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) throw new IllegalArgumentException("Choose a supported notes file.");
        String safe = filename.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (safe.isBlank() || safe.length() > 255 || safe.startsWith(".")) throw new IllegalArgumentException("Invalid file name.");
        return safe;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 1) throw new IllegalArgumentException("Supported files: PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT.");
        String ext = name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        if (!List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt").contains(ext))
            throw new IllegalArgumentException("Unsupported file type. Use PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX, or TXT.");
        return ext;
    }

    private String supportedContentType(String ext, MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) throw new IllegalArgumentException("Choose a non-empty notes file.");
            if (ext.equals("pdf") && !(bytes.length >= 5 && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")))
                throw new IllegalArgumentException("The file content does not match its PDF extension.");
            if (List.of("docx", "pptx", "xlsx").contains(ext) && !(bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K'))
                throw new IllegalArgumentException("The file content does not match its extension.");
            if (List.of("doc", "ppt", "xls").contains(ext) && !(bytes.length >= 4 && (bytes[0] & 255) == 0xD0 && (bytes[1] & 255) == 0xCF))
                throw new IllegalArgumentException("The file content does not match its extension.");
            if (ext.equals("txt")) for (byte b : bytes) if (b == 0) throw new IllegalArgumentException("The TXT file contains binary data.");
        } catch (IOException ex) { throw new IllegalArgumentException("The notes file could not be read."); }
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "text/plain";
        };
    }

    public long countResources() {
        return resourceRepository.count();
    }

    public Resource getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
        if (resource.isFileBacked()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        return resource;
    }

    public Resource updateResource(Long id, Resource submittedResource) {
        Resource resource = getResourceById(id);
        resource.setTitle(submittedResource.getTitle());
        resource.setSubject(submittedResource.getSubject());
        resource.setDescription(submittedResource.getDescription());
        resource.setUrl(submittedResource.getUrl());
        return resourceRepository.save(resource);
    }

    public void deleteResource(Long id) {
        resourceRepository.delete(getResourceById(id));
    }
}
