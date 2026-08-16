package com.campusmate.repository;

import com.campusmate.model.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByDateDescIdDesc();

    List<Notice> findTop3ByOrderByDateDescIdDesc();
}