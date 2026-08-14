package com.pixfactory.repo;

import com.pixfactory.domain.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {}
