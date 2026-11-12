package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;

import com.pulsepass.dto.response.EventResponse;

import com.pulsepass.dto.response.EventSummaryResponse;

import com.pulsepass.entity.Artist;

import com.pulsepass.entity.Event;

import com.pulsepass.entity.Venue;

import com.pulsepass.enums.EventCategory;

import com.pulsepass.enums.EventStatus;

import com.pulsepass.exception.BusinessRuleException;

import com.pulsepass.exception.DuplicateResourceException;

import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.mapper.EventMapper;

import com.pulsepass.repository.ArtistRepository;

import com.pulsepass.repository.EventRepository;

import com.pulsepass.repository.VenueRepository;

import com.pulsepass.service.impl.EventServiceImpl;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.List;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class EventServiceImplTest {

    @Mock

    EventRepository eventRepository;

    @Mock

    VenueRepository venueRepository;

    @Mock

    ArtistRepository artistRepository;

    @Mock

    EventMapper eventMapper;

    @InjectMocks

    EventServiceImpl eventService;

    @Test

    void create_shouldSaveValidEvent() {

        Venue venue = new Venue();

        venue.setCode("VEN-1");

        venue.setActive(true);

        CreateEventRequest request = new CreateEventRequest(

                "EV-1",

                "Concert",

                "desc",

                EventCategory.MUSIC,

                LocalDateTime.now().plusDays(10),

                18,

                "VEN-1"

        );

        Event saved = new Event();

        saved.setEventCode("EV-1");

        EventResponse response = mock(EventResponse.class);

        when(eventRepository.existsByEventCode("EV-1"))

                .thenReturn(false);

        when(venueRepository.findByCode("VEN-1"))

                .thenReturn(Optional.of(venue));

        when(eventRepository.save(any(Event.class)))

                .thenReturn(saved);

        when(eventMapper.toResponse(saved))

                .thenReturn(response);

        EventResponse result = eventService.create(request);

        assertThat(result).isSameAs(response);

        verify(eventRepository).save(any(Event.class));

    }

    @Test

    void create_shouldRejectDuplicateEventCode() {

        CreateEventRequest request = new CreateEventRequest(

                "EV-1",

                "Concert",

                "desc",

                EventCategory.MUSIC,

                LocalDateTime.now().plusDays(10),

                18,

                "VEN-1"

        );

        when(eventRepository.existsByEventCode("EV-1"))

                .thenReturn(true);

        assertThatThrownBy(() -> eventService.create(request))

                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());

    }

    @Test

    void create_shouldRejectWhenVenueDoesNotExist() {

        CreateEventRequest request = new CreateEventRequest(

                "EV-1",

                "Concert",

                "desc",

                EventCategory.MUSIC,

                LocalDateTime.now().plusDays(10),

                18,

                "VEN-1"

        );

        when(eventRepository.existsByEventCode("EV-1"))

                .thenReturn(false);

        when(venueRepository.findByCode("VEN-1"))

                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(request))

                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());

    }

    @Test

    void create_shouldRejectInactiveVenue() {

        Venue venue = new Venue();

        venue.setCode("VEN-1");

        venue.setActive(false);

        CreateEventRequest request = new CreateEventRequest(

                "EV-1",

                "Concert",

                "desc",

                EventCategory.MUSIC,

                LocalDateTime.now().plusDays(10),

                18,

                "VEN-1"

        );

        when(eventRepository.existsByEventCode("EV-1"))

                .thenReturn(false);

        when(venueRepository.findByCode("VEN-1"))

                .thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> eventService.create(request))

                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());

    }

    @Test

    void create_shouldRejectPastDate() {

        Venue venue = new Venue();

        venue.setCode("VEN-1");

        venue.setActive(true);

        CreateEventRequest request = new CreateEventRequest(

                "EV-1",

                "Concert",

                "desc",

                EventCategory.MUSIC,

                LocalDateTime.now().minusDays(1),

                18,

                "VEN-1"

        );

        when(eventRepository.existsByEventCode("EV-1"))

                .thenReturn(false);

        assertThatThrownBy(() -> eventService.create(request))

                .isInstanceOf(BusinessRuleException.class);

        verify(venueRepository, never()).findByCode(any());

        verify(eventRepository, never()).save(any());

    }

    @Test

    void findByCode_shouldReturnDto() {

        Event event = new Event();

        event.setEventCode("EV-1");

        EventResponse response = mock(EventResponse.class);

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.of(event));

        when(eventMapper.toResponse(event))

                .thenReturn(response);

        EventResponse result = eventService.findByCode("EV-1");

        assertThat(result).isSameAs(response);

        verify(eventRepository).findByEventCode("EV-1");

        verify(eventMapper).toResponse(event);

    }

    @Test

    void findByCode_shouldThrowWhenMissing() {

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByCode("EV-1"))

                .isInstanceOf(ResourceNotFoundException.class);

    }

    @Test

    void findPublishedEvents_shouldReturnDtos() {

        Event event = new Event();

        event.setEventCode("EV-1");

        event.setStatus(EventStatus.PUBLISHED);

        EventSummaryResponse response =

                mock(EventSummaryResponse.class);

        when(eventRepository.findByStatusOrderByEventDateAsc(

                EventStatus.PUBLISHED))

                .thenReturn(List.of(event));

        when(eventMapper.toSummary(event))

                .thenReturn(response);

        List<EventSummaryResponse> result =

                eventService.findPublishedEvents();

        assertThat(result)

                .containsExactly(response);

        verify(eventRepository)

                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        verify(eventMapper)

                .toSummary(event);

    }

    @Test

    void publish_shouldChangeDraftToPublished() {

        Event event = new Event();

        event.setEventCode("EV-1");

        event.setStatus(EventStatus.DRAFT);

        event.setEventDate(LocalDate.now().plusDays(2));

        Venue venue = new Venue();

        venue.setActive(true);

        event.setVenue(venue);

        EventResponse response =

                mock(EventResponse.class);

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.of(event));

        when(eventRepository.save(event))

                .thenReturn(event);

        when(eventMapper.toResponse(event))

                .thenReturn(response);

        EventResponse result =

                eventService.publish("EV-1");

        assertThat(event.getStatus())

                .isEqualTo(EventStatus.PUBLISHED);

        assertThat(result)

                .isSameAs(response);

        verify(eventRepository).save(event);

    }

    @Test

    void publish_shouldRejectCancelledEvent() {

        Event event = new Event();

        event.setEventCode("EV-1");

        event.setStatus(EventStatus.CANCELLED);

        event.setEventDate(LocalDate.now().plusDays(2));

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.publish("EV-1"))

                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());

    }

    @Test

    void addArtist_shouldAssociateArtist() {

        Event event = new Event();

        event.setEventCode("EV-1");

        event.setStatus(EventStatus.DRAFT);

        Artist artist = new Artist();

        artist.setId(1L);

        EventResponse response =

                mock(EventResponse.class);

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.of(event));

        when(artistRepository.findById(1L))

                .thenReturn(Optional.of(artist));

        when(eventRepository.save(event))

                .thenReturn(event);

        when(eventMapper.toResponse(event))

                .thenReturn(response);

        EventResponse result =

                eventService.addArtist("EV-1", 1L);

        assertThat(event.getArtists())

                .contains(artist);

        assertThat(result)

                .isSameAs(response);

        verify(eventRepository)

                .save(event);

    }

    @Test

    void addArtist_shouldRejectDuplicateArtist() {

        Event event = new Event();

        event.setEventCode("EV-1");

        event.setStatus(EventStatus.DRAFT);

        Artist artist = new Artist();

        artist.setId(1L);

        event.getArtists().add(artist);

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.of(event));

        when(artistRepository.findById(1L))

                .thenReturn(Optional.of(artist));

        assertThatThrownBy(

                () -> eventService.addArtist("EV-1", 1L)

        )

                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());

    }

    @Test

    void addArtist_shouldRejectCancelledEvent() {

        Event event = new Event();

        event.setEventCode("EV-1");

        event.setStatus(EventStatus.CANCELLED);

        when(eventRepository.findByEventCode("EV-1"))

                .thenReturn(Optional.of(event));

        assertThatThrownBy(

                () -> eventService.addArtist("EV-1", 1L)

        )

                .isInstanceOf(BusinessRuleException.class);

        verify(artistRepository, never()).findById(any());

        verify(eventRepository, never()).save(any());

    }

    @Test

    void findByArtist_shouldReturnDtos() {

        Event event = new Event();

        event.setEventCode("EV-1");

        EventSummaryResponse response =

                mock(EventSummaryResponse.class);

        when(eventRepository.findByArtistStageName("The Artist"))

                .thenReturn(List.of(event));

        when(eventMapper.toSummary(event))

                .thenReturn(response);

        List<EventSummaryResponse> result =

                eventService.findByArtist("The Artist");

        assertThat(result)

                .containsExactly(response);

        verify(eventRepository)

                .findByArtistStageName("The Artist");

        verify(eventMapper)

                .toSummary(event);

    }


    @Test
    void publish_shouldRejectPastEvent() {
        Event event = new Event();
        event.setEventCode("EV-PAST");
        event.setStatus(EventStatus.DRAFT);
        event.setEventDate(LocalDate.now().minusDays(1));

        Venue venue = new Venue();
        venue.setActive(true);
        event.setVenue(venue);

        when(eventRepository.findByEventCode("EV-PAST")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.publish("EV-PAST"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void publish_shouldRejectWhenVenueIsInactive() {
        Event event = new Event();
        event.setEventCode("EV-INACTIVE-VENUE");
        event.setStatus(EventStatus.DRAFT);
        event.setEventDate(LocalDate.now().plusDays(5));

        Venue venue = new Venue();
        venue.setActive(false);
        event.setVenue(venue);

        when(eventRepository.findByEventCode("EV-INACTIVE-VENUE")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.publish("EV-INACTIVE-VENUE"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addArtist_shouldRejectFinishedEvent() {
        Event event = new Event();
        event.setEventCode("EV-FINISHED");
        event.setStatus(EventStatus.FINISHED);

        when(eventRepository.findByEventCode("EV-FINISHED")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.addArtist("EV-FINISHED", 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(artistRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }


    @Test
    void create_shouldRejectNegativeMinimumAge() {
        when(eventRepository.existsByEventCode("EV-NEGATIVE-AGE")).thenReturn(false);

        CreateEventRequest request = new CreateEventRequest(
                "EV-NEGATIVE-AGE",
                "Concert",
                "desc",
                EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10),
                -1,
                "VEN-1"
        );

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Minimum age");

        verify(venueRepository, never()).findByCode(any());
        verify(eventRepository, never()).save(any());
    }

}

