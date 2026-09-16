package com.tripplanner.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRevisionRepository extends JpaRepository<PromptTemplateRevision, UUID> {

    List<PromptTemplateRevision> findByTemplateIdOrderByVersionDesc(UUID templateId);

    Optional<PromptTemplateRevision> findByTemplateIdAndVersion(UUID templateId, int version);
}
