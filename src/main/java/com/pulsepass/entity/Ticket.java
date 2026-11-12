package com.pulsepass.entity;

import com.pulsepass.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="ticket_code", nullable=false, unique=true, length=50)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private TicketType type;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private TicketStatus status;

    @Column(name="purchase_date", nullable=false)
    private LocalDate purchaseDate;

    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne
    @JoinColumn(name="event_id", nullable=false)
    private Event event;

    public Ticket(){}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }

    public String getTicketCode(){ return ticketCode; }
    public void setTicketCode(String ticketCode){ this.ticketCode=ticketCode; }

    public TicketType getType(){ return type; }
    public void setType(TicketType type){ this.type=type; }

    public BigDecimal getPrice(){ return price; }
    public void setPrice(BigDecimal price){ this.price=price; }

    public TicketStatus getStatus(){ return status; }
    public void setStatus(TicketStatus status){ this.status=status; }

    public LocalDate getPurchaseDate(){ return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate){ this.purchaseDate=purchaseDate; }

    public User getUser(){ return user; }
    public void setUser(User user){ this.user=user; }

    public Event getEvent(){ return event; }
    public void setEvent(Event event){ this.event=event; }
}
