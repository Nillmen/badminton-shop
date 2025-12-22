package com.badmintonshop.repository;

import com.badmintonshop.entity.StringingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StringingServiceRepository
        extends JpaRepository<StringingService, Long> {

    @Query(
            value = """
        SELECT
            service_id,
            service_name,
            CASE service_type
                WHEN 'standard' THEN 'STANDARD'
                WHEN '4_knot'   THEN 'FOUR_KNOT'
                WHEN '2_knot'   THEN 'TWO_KNOT'
                ELSE service_type
            END AS service_type,
            description,
            base_price,
            estimated_time_minutes,
            is_active,
            created_at,
            updated_at,
            deleted_at
        FROM stringing_services
        WHERE is_active = 1
          AND deleted_at IS NULL
        """,
            nativeQuery = true
    )
    List<StringingService> findAllActive();

    @Query(
            value = """
        SELECT
            service_id,
            service_name,
            CASE service_type
                WHEN 'standard' THEN 'STANDARD'
                WHEN '4_knot'   THEN 'FOUR_KNOT'
                WHEN '2_knot'   THEN 'TWO_KNOT'
                ELSE service_type
            END AS service_type,
            description,
            base_price,
            estimated_time_minutes,
            is_active
        FROM stringing_services
        WHERE service_id = :id
          AND deleted_at IS NULL
        """,
            nativeQuery = true
    )
    Object findDtoById(@Param("id") Long id);

    List<StringingService> findByIsActiveTrue();

}
