package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;

import java.util.List;

public interface EventService {

    EventResponse create(CreateEventRequest request);

    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    EventResponse publish(String eventCode);

    EventResponse addArtist(String eventCode, Long artistId);

    List<EventSummaryResponse> findByArtist(String stageName);

    List<EventSummaryResponse> findPublishedByArtist(String stageName);
}