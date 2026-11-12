package com.pulsepass.entity;

import com.pulsepass.enums.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="events")
public class Event {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="event_code", nullable=false, unique=true, length=50)
    private String eventCode;

    @Column(nullable=false, length=150)
    private String name;

    @Column(columnDefinition="TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private EventStatus status;

    @Column(name="event_date", nullable=false)
    private LocalDate eventDate;
    @Column(name="minimum_age", nullable=false)
    private Integer minimumAge;
    @Column(name="streaming_url", length=500)
    private String streamingUrl;

    @ManyToOne
    @JoinColumn(name="venue_id", nullable=false)
    private Venue venue;

    @ManyToMany
    @JoinTable(
            name="event_artists",
            joinColumns=@JoinColumn(name="event_id"),
            inverseJoinColumns=@JoinColumn(name="artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    public Event(){}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }

    public String getEventCode(){ return eventCode; }
    public void setEventCode(String eventCode){ this.eventCode=eventCode; }

    public String getName(){ return name; }
    public void setName(String name){ this.name=name; }

    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description=description; }

    public EventCategory getCategory(){ return category; }
    public void setCategory(EventCategory category){ this.category=category; }

    public EventStatus getStatus(){ return status; }
    public void setStatus(EventStatus status){ this.status=status; }

    public LocalDate getEventDate(){ return eventDate; }
    public void setEventDate(LocalDate eventDate){ this.eventDate=eventDate; }

    public Integer getMinimumAge(){ return minimumAge; }
    public void setMinimumAge(Integer minimumAge){ this.minimumAge=minimumAge; }

    public String getStreamingUrl(){ return streamingUrl; }
    public void setStreamingUrl(String streamingUrl){ this.streamingUrl=streamingUrl; }

    public Venue getVenue(){ return venue; }
    public void setVenue(Venue venue){ this.venue=venue; }

    public Set<Artist> getArtists(){ return artists; }
    public void setArtists(Set<Artist> artists){ this.artists=artists; }
}
