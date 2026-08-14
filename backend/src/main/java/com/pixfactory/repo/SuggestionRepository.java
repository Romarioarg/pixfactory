package com.pixfactory.repo;

import com.pixfactory.domain.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {}
