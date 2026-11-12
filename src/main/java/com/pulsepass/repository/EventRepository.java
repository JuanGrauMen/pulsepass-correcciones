package com.pulsepass.repository;

import com.pulsepass.entity.Event;
import com.pulsepass.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventCode(String eventCode);
    boolean existsByEventCode(String eventCode);
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);
    List<Event> findByVenueCode(String code);

    @Query("""
        SELECT DISTINCT e FROM Event e JOIN e.artists a
        WHERE LOWER(a.stageName) = LOWER(:stageName)
        """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a WHERE LOWER(a.stageName) = LOWER(:stageName) AND e.status = :status")
    List<Event> findByArtistStageNameAndStatus(@Param("stageName") String stageName, @Param("status") EventStatus status);

    @Query("""
        SELECT DISTINCT e FROM Event e JOIN e.artists a JOIN e.venue v
        WHERE v.city = :city AND LOWER(a.stageName) = LOWER(:stageName)
        """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                              @Param("stageName") String stageName);

    @Query("""
        SELECT DISTINCT e FROM Event e JOIN e.artists a JOIN e.venue v
        WHERE e.status = :status AND e.eventDate > :fromDate
          AND v.city = :city
          AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
        ORDER BY e.eventDate ASC
        """)
    List<Event> findRecommendedEvents(@Param("status") EventStatus status,
                                      @Param("fromDate") LocalDate fromDate,
                                      @Param("city") String city,
                                      @Param("artistText") String artistText);
}
