package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Ticket;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserProfile;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.enums.TicketStatus;
import com.pulsepass.enums.TicketType;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String USER_EMAIL = "a@x.com";
    private static final String EVENT_CODE = "EV-1";
    private static final String TICKET_CODE = "TCK-TEST-001";

    @Mock
    TicketRepository ticketRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    TicketMapper ticketMapper;

    @InjectMocks
    TicketServiceImpl ticketService;

    @Test
    void purchase_shouldRejectWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).findByEventCode(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_shouldRejectInactiveUser() {
        User user = user(false, null);
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");

        verify(eventRepository, never()).findByEventCode(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_shouldRejectWhenEventDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).countByEventEventCodeAndStatus(any(), any());
        verify(ticketRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"DRAFT", "SOLD_OUT", "CANCELLED", "FINISHED"})
    void purchase_shouldRejectWhenEventIsNotPublished(EventStatus status) {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(status, 10, LocalDate.now().plusDays(10))));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_shouldRejectWhenEventDateIsInPast() {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(
                EventStatus.PUBLISHED, 10, LocalDate.now().minusDays(1))));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already occurred");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_shouldRejectWhenAgeRestrictedEventHasNoBirthDate() {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user(true, null)));
        Event event = event(EventStatus.PUBLISHED, 10, LocalDate.now().plusDays(30));
        event.setMinimumAge(18);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("birth date is required");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_shouldRejectWhenUserDoesNotMeetMinimumAge() {
        User user = user(true, LocalDate.now().minusYears(16));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        Event event = event(EventStatus.PUBLISHED, 10, LocalDate.now().plusDays(30));
        event.setMinimumAge(18);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void purchase_shouldRejectWhenCapacityIsAlreadyReached() {
        User user = user(true, null);
        Event event = event(EventStatus.PUBLISHED, 2, LocalDate.now().plusDays(10));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(2L);

        assertThatThrownBy(() -> ticketService.purchase(request(TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sold out");

        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void purchase_shouldSavePaidTicketAndCalculateGeneralPrice() {
        User user = user(true, null);
        Event event = event(EventStatus.PUBLISHED, 10, LocalDate.now().plusDays(10));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response(TicketType.GENERAL, TicketStatus.PAID));

        TicketResponse result = ticketService.purchase(request(TicketType.GENERAL));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();

        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getTicketCode()).startsWith("TCK-");
        assertThat(saved.getType()).isEqualTo(TicketType.GENERAL);
        assertThat(saved.getPrice()).isEqualByComparingTo("120000");
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getPurchaseDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getEvent()).isSameAs(event);
        verify(eventRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "GENERAL, 120000",
            "STUDENT, 84000",
            "VIP, 250000",
            "BACKSTAGE, 350000"
    })
    void purchase_shouldApplyPriceForEachTicketType(TicketType type, String expectedPrice) {
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event(
                EventStatus.PUBLISHED, 100, LocalDate.now().plusDays(10))));
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response(type, TicketStatus.PAID));

        ticketService.purchase(request(type));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo(expectedPrice);
    }

    @Test
    void purchase_shouldMarkEventSoldOutWhenLastTicketIsSold() {
        User user = user(true, null);
        Event event = event(EventStatus.PUBLISHED, 2, LocalDate.now().plusDays(10));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(1L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response(TicketType.GENERAL, TicketStatus.PAID));

        ticketService.purchase(request(TicketType.GENERAL));

        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void findByCode_shouldReturnMappedTicket() {
        Ticket ticket = ticket(TicketStatus.PAID, LocalDate.now().plusDays(10));
        TicketResponse response = response(TicketType.GENERAL, TicketStatus.PAID);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        assertThat(ticketService.findByCode(TICKET_CODE)).isSameAs(response);
    }

    @Test
    void findByCode_shouldThrowWhenTicketDoesNotExist() {
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.findByCode(TICKET_CODE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUserEmail_shouldReturnMappedTickets() {
        Ticket ticket = ticket(TicketStatus.PAID, LocalDate.now().plusDays(10));
        TicketResponse response = response(TicketType.GENERAL, TicketStatus.PAID);
        when(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(USER_EMAIL))
                .thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        assertThat(ticketService.findByUserEmail(USER_EMAIL)).containsExactly(response);
    }

    @Test
    void findPaidTicketsByEvent_shouldReturnMappedTickets() {
        Ticket ticket = ticket(TicketStatus.PAID, LocalDate.now().plusDays(10));
        TicketResponse response = response(TicketType.GENERAL, TicketStatus.PAID);
        when(ticketRepository.findByEventEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID))
                .thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        assertThat(ticketService.findPaidTicketsByEvent(EVENT_CODE)).containsExactly(response);
    }

    @Test
    void cancel_shouldChangePaidTicketToCancelled() {
        Ticket ticket = ticket(TicketStatus.PAID, LocalDate.now().plusDays(10));
        TicketResponse response = response(TicketType.GENERAL, TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        assertThat(ticketService.cancel(TICKET_CODE)).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESERVED", "CANCELLED", "USED"})
    void cancel_shouldRejectWhenTicketIsNotPaid(TicketStatus status) {
        when(ticketRepository.findByTicketCode(TICKET_CODE))
                .thenReturn(Optional.of(ticket(status, LocalDate.now().plusDays(10))));

        assertThatThrownBy(() -> ticketService.cancel(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancel_shouldRejectWhenEventDateHasPassed() {
        when(ticketRepository.findByTicketCode(TICKET_CODE))
                .thenReturn(Optional.of(ticket(TicketStatus.PAID, LocalDate.now().minusDays(1))));

        assertThatThrownBy(() -> ticketService.cancel(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("after the event date");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void markAsUsed_shouldChangePaidTicketToUsed() {
        Ticket ticket = ticket(TicketStatus.PAID, LocalDate.now().plusDays(10));
        TicketResponse response = response(TicketType.GENERAL, TicketStatus.USED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        assertThat(ticketService.markAsUsed(TICKET_CODE)).isSameAs(response);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(ticket);
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"RESERVED", "CANCELLED", "USED"})
    void markAsUsed_shouldRejectWhenTicketIsNotPaid(TicketStatus status) {
        when(ticketRepository.findByTicketCode(TICKET_CODE))
                .thenReturn(Optional.of(ticket(status, LocalDate.now().plusDays(10))));

        assertThatThrownBy(() -> ticketService.markAsUsed(TICKET_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PAID");

        verify(ticketRepository, never()).save(any());
    }

    private PurchaseTicketRequest request(TicketType type) {
        return new PurchaseTicketRequest(USER_EMAIL, EVENT_CODE, type);
    }

    private User user(boolean active, LocalDate birthDate) {
        User user = new User();
        user.setId(1L);
        user.setUsername("user-test");
        user.setEmail(USER_EMAIL);
        user.setActive(active);
        if (birthDate != null) {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setBirthDate(birthDate);
            user.setProfile(profile);
        }
        return user;
    }

    private Event event(EventStatus status, int capacity, LocalDate eventDate) {
        Venue venue = new Venue();
        venue.setCode("VEN-TEST");
        venue.setName("Venue Test");
        venue.setCapacity(capacity);
        venue.setActive(true);

        Event event = new Event();
        event.setEventCode(EVENT_CODE);
        event.setName("Event Test");
        event.setStatus(status);
        event.setEventDate(eventDate);
        event.setMinimumAge(0);
        event.setVenue(venue);
        return event;
    }

    private Ticket ticket(TicketStatus status, LocalDate eventDate) {
        User user = user(true, null);
        Event event = event(EventStatus.PUBLISHED, 10, eventDate);
        Ticket ticket = new Ticket();
        ticket.setTicketCode(TICKET_CODE);
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("120000"));
        ticket.setStatus(status);
        ticket.setPurchaseDate(LocalDate.now());
        ticket.setUser(user);
        ticket.setEvent(event);
        return ticket;
    }

    private TicketResponse response(TicketType type, TicketStatus status) {
        return new TicketResponse(
                1L,
                TICKET_CODE,
                type,
                new BigDecimal("120000"),
                status,
                LocalDate.now(),
                USER_EMAIL,
                EVENT_CODE,
                "Event Test"
        );
    }
}
