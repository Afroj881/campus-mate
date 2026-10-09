package com.campusmate.repository;

import com.campusmate.model.Resource;
import com.campusmate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findAllByOrderBySubjectAscTitleAsc();

    List<Resource> findAllByUploadedByIsNullOrderBySubjectAscTitleAsc();

    @Query("select resource from Resource resource where resource.uploadedBy is null and (resource.department is null or lower(trim(resource.department)) = lower(trim(:department))) and (resource.semester is null or resource.semester = :semester) and (resource.section is null or lower(trim(resource.section)) = lower(trim(:section))) order by resource.subject asc, resource.title asc")
    List<Resource> findUrlResourcesForClass(String department, Integer semester, String section);

    long countByUploadedByIsNull();

    @Query("select resource from Resource resource join fetch resource.uploadedBy where resource.uploadedBy = :faculty order by resource.createdAt desc")
    List<Resource> findFilesByUploadedBy(User faculty);

    @Query("select resource from Resource resource join fetch resource.uploadedBy where resource.id = :id")
    Optional<Resource> findWithUploaderById(Long id);

    @Query("select resource from Resource resource join fetch resource.uploadedBy where resource.uploadedBy = :faculty and resource.id = :id")
    Optional<Resource> findOwnedFile(Long id, User faculty);

    @Query("select resource from Resource resource join fetch resource.uploadedBy where resource.uploadedBy is not null and lower(trim(resource.department)) = lower(trim(:department)) and resource.semester = :semester and lower(trim(resource.section)) = lower(trim(:section)) order by resource.createdAt desc, resource.title asc")
    List<Resource> findFilesForClass(String department, Integer semester, String section);

    @Query("select resource from Resource resource where resource.uploadedBy is null and (lower(resource.title) like lower(concat('%', :term, '%')) or lower(resource.subject) like lower(concat('%', :term, '%')) or lower(resource.description) like lower(concat('%', :term, '%'))) ")
    List<Resource> searchUrlResources(String term);

    @Query("select resource from Resource resource where resource.uploadedBy is null and (resource.department is null or lower(trim(resource.department)) = lower(trim(:department))) and (resource.semester is null or resource.semester = :semester) and (resource.section is null or lower(trim(resource.section)) = lower(trim(:section))) and (lower(resource.title) like lower(concat('%', :term, '%')) or lower(resource.subject) like lower(concat('%', :term, '%')) or lower(resource.description) like lower(concat('%', :term, '%'))) ")
    List<Resource> searchUrlResourcesForClass(String term, String department, Integer semester, String section);
}
