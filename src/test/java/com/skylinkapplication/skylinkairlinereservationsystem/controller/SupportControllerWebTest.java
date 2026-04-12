package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupportController.class)
public class SupportControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportService supportService;

    private SupportTicketDTO sampleTicket;

    @BeforeEach
    public void setUp() {
        sampleTicket = new SupportTicketDTO();
        sampleTicket.setId(1L);
        sampleTicket.setIssueTitle("Sample Issue");
        sampleTicket.setIssueDescription("This is a sample issue description");
        sampleTicket.setStatus("OPEN");
        sampleTicket.setCreatedDate(new Date());
        sampleTicket.setUserId(1L);
    }

    @Test
    public void testCustomerSupportDashboard() throws Exception {
        when(supportService.getAllTickets()).thenReturn(List.of(sampleTicket));

        mockMvc.perform(get("/api/support/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-support-dashboard"))
                .andExpect(model().attributeExists("tickets"))
                .andExpect(model().attribute("tickets", List.of(sampleTicket)));
    }

    @Test
    public void testGetTicketsJson() throws Exception {
        when(supportService.getAllTickets()).thenReturn(List.of(sampleTicket));

        mockMvc.perform(get("/api/support/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}