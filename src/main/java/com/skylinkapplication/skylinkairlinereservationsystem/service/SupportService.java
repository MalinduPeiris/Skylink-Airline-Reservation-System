package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.SupportTicket;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportService {

	@Autowired
	private SupportTicketRepository supportTicketRepository;

	public List<SupportTicketDTO> getAllTickets() {
		return supportTicketRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	public void respondToTicket(Long ticketId, String response) {
		SupportTicket ticket = supportTicketRepository.findById(ticketId)
				.orElseThrow(() -> new RuntimeException("Ticket not found"));
		ticket.setStatus(SupportTicket.Status.RESOLVED);
		// Add response logic (e.g., update description or add comment field)
		supportTicketRepository.save(ticket);
	}

	private SupportTicketDTO convertToDTO(SupportTicket ticket) {
		SupportTicketDTO dto = new SupportTicketDTO();
		dto.setId(ticket.getId());
		dto.setIssueDescription(ticket.getIssueDescription());
		dto.setStatus(ticket.getStatus().name());
		dto.setCreatedDate(ticket.getCreatedDate());
		dto.setUserId(ticket.getUser() != null ? ticket.getUser().getId() : null);
		return dto;
	}
}