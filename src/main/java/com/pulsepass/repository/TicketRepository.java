package com.pulsepass.repository;

import com.pulsepass.entity.Ticket;
import com.pulsepass.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email);
    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);
    List<Ticket> findByUserEmailIgnoreCaseAndStatus(String email, TicketStatus status);
    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);
    long countByEventEventCodeAndStatus(String eventCode, TicketStatus status);
    List<Ticket> findByEventEventDateAfterOrderByEventEventDateAsc(java.time.LocalDate fromDate);

    @Query("""
    SELECT COUNT(t)
    FROM Ticket t
    WHERE t.event.eventCode = :eventCode
      AND t.status = :status
""")
    long countByEventCodeAndStatus(
            @Param("eventCode") String eventCode,
            @Param("status") TicketStatus status
    );
}
