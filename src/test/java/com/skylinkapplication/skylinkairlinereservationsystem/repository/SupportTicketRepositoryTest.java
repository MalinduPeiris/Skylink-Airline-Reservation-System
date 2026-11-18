package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.SupportTicket;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:support_ticket_repository_test;MODE=MySQL;NON_KEYWORDS=USER",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SupportTicketRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;


    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Test
    public void testFindAll() {
        User user = new User();
        user.setUsername("support-traveler");
        user.setEmail("support-traveler@example.com");
        user.setPassword("test-only-password");
        user.setRole(Role.FREQUENT_TRAVELER);
        entityManager.persistAndFlush(user);

        SupportTicket ticket = new SupportTicket();
        ticket.setIssueTitle("Seat question");
        ticket.setIssueDescription("Please confirm the seat assignment");
        ticket.setCreatedDate(new Date());
        ticket.setStatus(SupportTicket.Status.OPEN);
        ticket.setUser(user);
        entityManager.persistAndFlush(ticket);
        entityManager.clear();

        List<SupportTicket> tickets = supportTicketRepository.findAll();

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getIssueTitle()).isEqualTo("Seat question");
        assertThat(tickets.get(0).getStatus()).isEqualTo(SupportTicket.Status.OPEN);
    }
}
