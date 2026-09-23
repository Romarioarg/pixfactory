package com.pixfactory.repo;

import com.pixfactory.domain.CashSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {
    Optional<CashSession> findFirstByDateAndStatus(LocalDate date, String status);
    Optional<CashSession> findFirstByStatusOrderByOpenedAtDesc(String status);
    List<CashSession> findAllByOrderByDateDesc();
}
