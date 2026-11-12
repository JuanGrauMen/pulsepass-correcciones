package com.pulsepass.service.impl;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.entity.Artist;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            ArtistRepository artistRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException(
                    "An event with code '" + request.eventCode() + "' already exists"
            );
        }

        if (request.minimumAge() < 0) {
            throw new BusinessRuleException(
                    "Minimum age cannot be negative"
            );
        }

        if (!request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Event date must be in the future"
            );
        }

        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue with code '" + request.venueCode() + "' was not found"
                ));

        if (!venue.getActive()) {
            throw new BusinessRuleException(
                    "Venue with code '" + request.venueCode() + "' is inactive"
            );
        }

        Event event = new Event();

        event.setEventCode(request.eventCode());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setStatus(EventStatus.DRAFT);
        event.setEventDate(request.eventDate());
        event.setMinimumAge(request.minimumAge());
        event.setVenue(venue);

        Event savedEvent = eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event with code '" + eventCode + "' was not found"
                ));

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {

        return eventRepository
                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event with code '" + eventCode + "' was not found"
                ));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published"
            );
        }

        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Event date must be in the future"
            );
        }

        if (event.getVenue() == null || !event.getVenue().getActive()) {
            throw new BusinessRuleException(
                    "Event venue must be active"
            );
        }

        event.setStatus(EventStatus.PUBLISHED);

        Event savedEvent = eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event with code '" + eventCode + "' was not found"
                ));

        if (event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Artists cannot be added to a cancelled or finished event"
            );
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artist with id '" + artistId + "' was not found"
                ));

        boolean alreadyAdded = event.getArtists()
                .stream()
                .anyMatch(existingArtist ->
                        existingArtist.getId().equals(artistId));

        if (alreadyAdded) {
            throw new BusinessRuleException(
                    "Artist is already associated with the event"
            );
        }

        event.getArtists().add(artist);

        Event savedEvent = eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName).stream()
                .map(eventMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedByArtist(String stageName) {
        return eventRepository.findByArtistStageNameAndStatus(stageName, EventStatus.PUBLISHED)
                .stream().map(eventMapper::toSummary).toList();
    }
}