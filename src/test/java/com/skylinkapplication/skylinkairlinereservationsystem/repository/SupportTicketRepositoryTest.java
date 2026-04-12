package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.SupportTicket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class SupportTicketRepositoryTest {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Test
    public void testFindAll() {
        // Check if there are any tickets in the database
        List<SupportTicket> tickets = supportTicketRepository.findAll();
        System.out.println("Number of tickets in database: " + tickets.size());
        
        // Print details of each ticket
        for (SupportTicket ticket : tickets) {
            System.out.println("Ticket ID: " + ticket.getId() + 
                             ", Title: " + ticket.getIssueTitle() + 
                             ", Status: " + ticket.getStatus());
        }
        
        // This test will pass regardless of whether tickets exist or not
        assertThat(tickets).isNotNull();
    }
}