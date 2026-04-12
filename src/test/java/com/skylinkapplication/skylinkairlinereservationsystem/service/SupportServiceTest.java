package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.SupportTicket;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.SupportTicketRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupportService supportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllTickets() {
        // Create sample support ticket
        SupportTicket ticket = new SupportTicket();
        ticket.setId(1L);
        ticket.setIssueTitle("Test Issue");
        ticket.setIssueDescription("Test Description");
        ticket.setStatus(SupportTicket.Status.OPEN);
        ticket.setCreatedDate(new Date());
        
        User user = new User();
        user.setId(1L);
        ticket.setUser(user);

        when(supportTicketRepository.findAll()).thenReturn(List.of(ticket));

        List<SupportTicketDTO> tickets = supportService.getAllTickets();

        assertNotNull(tickets);
        assertEquals(1, tickets.size());
        assertEquals("Test Issue", tickets.get(0).getIssueTitle());
        assertEquals("Test Description", tickets.get(0).getIssueDescription());
        assertEquals("OPEN", tickets.get(0).getStatus());
    }

    @Test
    void testConvertToDTO() {
        // This tests the private method indirectly through the public method
        SupportTicket ticket = new SupportTicket();
        ticket.setId(1L);
        ticket.setIssueTitle("Test Issue");
        ticket.setIssueDescription("Test Description");
        ticket.setStatus(SupportTicket.Status.RESOLVED);
        ticket.setCreatedDate(new Date());
        
        User user = new User();
        user.setId(1L);
        ticket.setUser(user);

        when(supportTicketRepository.findAll()).thenReturn(List.of(ticket));

        List<SupportTicketDTO> tickets = supportService.getAllTickets();

        assertNotNull(tickets);
        assertEquals(1, tickets.size());
        assertEquals("Test Issue", tickets.get(0).getIssueTitle());
        assertEquals("Test Description", tickets.get(0).getIssueDescription());
        assertEquals("RESOLVED", tickets.get(0).getStatus());
        assertEquals(1L, tickets.get(0).getUserId());
    }
}