package com.tripplanner.common;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, UUID> {

    /** 코드별 발생 횟수: [error_code, error_category, count] 내림차순. */
    @Query("""
            select e.errorCode, e.errorCategory, count(e)
            from ErrorLog e
            group by e.errorCode, e.errorCategory
            order by count(e) desc, e.errorCode asc
            """)
    List<Object[]> aggregateByCode();

    List<ErrorLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
