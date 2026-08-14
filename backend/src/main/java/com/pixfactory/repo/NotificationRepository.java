package com.pixfactory.repo;

import com.pixfactory.domain.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<AppNotification, Long> {}
