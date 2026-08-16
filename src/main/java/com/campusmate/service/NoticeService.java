package com.campusmate.service;

import com.campusmate.model.Notice;
import com.campusmate.repository.NoticeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public Notice createNotice(Notice notice) {
        notice.setId(null);
        return noticeRepository.save(notice);
    }

    public List<Notice> getAllNotices() {
        return noticeRepository.findAllByOrderByDateDescIdDesc();
    }

    public List<Notice> getLatestNotices() {
        return noticeRepository.findTop3ByOrderByDateDescIdDesc();
    }

    public Notice getNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notice not found"));
    }

    public Notice updateNotice(Long id, Notice notice) {
        Notice existingNotice = getNoticeById(id);
        existingNotice.setTitle(notice.getTitle());
        existingNotice.setDescription(notice.getDescription());
        existingNotice.setCategory(notice.getCategory());
        existingNotice.setDate(notice.getDate());
        return noticeRepository.save(existingNotice);
    }

    public void deleteNotice(Long id) {
        noticeRepository.delete(getNoticeById(id));
    }

    public long countNotices() {
        return noticeRepository.count();
    }
}