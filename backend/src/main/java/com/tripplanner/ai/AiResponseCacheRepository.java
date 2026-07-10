package com.tripplanner.ai;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiResponseCacheRepository extends JpaRepository<AiResponseCache, UUID> {

    Optional<AiResponseCache> findByCacheKey(String cacheKey);
}
