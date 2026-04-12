package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.SupportTicket;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.SupportTicketRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportService {

	@Autowired
	private SupportTicketRepository supportTicketRepository;

	@Autowired
	private UserRepository userRepository;

	@Transactional
	public SupportTicketDTO createSupportTicket(SupportTicketDTO ticketDTO) {
		// Validate required fields
		if (ticketDTO.getIssueTitle() == null || ticketDTO.getIssueTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Issue title is required");
		}
		if (ticketDTO.getIssueDescription() == null || ticketDTO.getIssueDescription().trim().isEmpty()) {
			throw new IllegalArgumentException("Issue description is required");
		}
		if (ticketDTO.getUserId() == null) {
			throw new IllegalArgumentException("User ID is required");
		}

		User user = userRepository.findById(ticketDTO.getUserId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		SupportTicket ticket = new SupportTicket();
		ticket.setIssueTitle(ticketDTO.getIssueTitle().trim());
		ticket.setIssueDescription(ticketDTO.getIssueDescription().trim());
		ticket.setStatus(SupportTicket.Status.OPEN);
		ticket.setCreatedDate(new Date());
		ticket.setUser(user);

		SupportTicket savedTicket = supportTicketRepository.save(ticket);
		return convertToDTO(savedTicket);
	}

	@Transactional
	public void createSampleTickets() {
		if (supportTicketRepository.count() > 0) {
			return;
		}

		List<User> users = userRepository.findAll();
		User user1, user2;
		if (users.size() >= 2) {
			user1 = users.get(0);
			user2 = users.get(1);
		} else {
			user1 = new User();
			user1.setUsername("john_doe");
			user1.setEmail("john.doe@example.com");
			user1.setPassword("password");
			user1 = userRepository.save(user1);

			user2 = new User();
			user2.setUsername("jane_smith");
			user2.setEmail("jane.smith@example.com");
			user2.setPassword("password");
			user2 = userRepository.save(user2);
		}

		SupportTicket ticket1 = new SupportTicket();
		ticket1.setIssueTitle("Billing Discrepancy");
		ticket1.setIssueDescription("I was charged twice for the same service last month. Please investigate and refund the duplicate charge.");
		ticket1.setStatus(SupportTicket.Status.OPEN);
		ticket1.setCreatedDate(new Date());
		ticket1.setUser(user1);
		supportTicketRepository.save(ticket1);

		SupportTicket ticket2 = new SupportTicket();
		ticket2.setIssueTitle("Delayed Flight Compensation");
		ticket2.setIssueDescription("My flight was delayed by 4 hours and I wasn't offered any compensation. I would like to discuss possible reimbursement options.");
		ticket2.setStatus(SupportTicket.Status.RESOLVED);
		ticket2.setCreatedDate(new Date(System.currentTimeMillis() - 86400000));
		ticket2.setUser(user2);
		supportTicketRepository.save(ticket2);

		SupportTicket ticket3 = new SupportTicket();
		ticket3.setIssueTitle("Lost Luggage");
		ticket3.setIssueDescription("My luggage was lost during my connecting flight. I've been waiting for updates for 3 days and haven't received any information.");
		ticket3.setStatus(SupportTicket.Status.OPEN);
		ticket3.setCreatedDate(new Date(System.currentTimeMillis() - 172800000));
		ticket3.setUser(user1);
		supportTicketRepository.save(ticket3);
	}

	@Transactional
	public SupportTicketDTO updateSupportTicket(Long id, SupportTicketDTO updatedTicketDTO) {
		// Validate ID
		if (id == null) {
			throw new IllegalArgumentException("Ticket ID is required");
		}

		SupportTicket ticket = supportTicketRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Ticket not found"));

		// Validate and update fields if provided
		if (updatedTicketDTO.getIssueTitle() != null && !updatedTicketDTO.getIssueTitle().trim().isEmpty()) {
			ticket.setIssueTitle(updatedTicketDTO.getIssueTitle().trim());
		}
		
		if (updatedTicketDTO.getIssueDescription() != null && !updatedTicketDTO.getIssueDescription().trim().isEmpty()) {
			ticket.setIssueDescription(updatedTicketDTO.getIssueDescription().trim());
		}
		
		if (updatedTicketDTO.getStatus() != null && !updatedTicketDTO.getStatus().trim().isEmpty()) {
			try {
				ticket.setStatus(SupportTicket.Status.valueOf(updatedTicketDTO.getStatus().toUpperCase().trim()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid ticket status: " + updatedTicketDTO.getStatus());
			}
		}

		SupportTicket updatedTicket = supportTicketRepository.save(ticket);
		return convertToDTO(updatedTicket);
	}

	public SupportTicketDTO getSupportTicketById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Ticket ID is required");
		}
		
		SupportTicket ticket = supportTicketRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Ticket not found"));
		return convertToDTO(ticket);
	}

	public List<SupportTicketDTO> getAllTickets() {
		return supportTicketRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	@Transactional
	public void deleteSupportTicket(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Ticket ID is required");
		}
		
		SupportTicket ticket = supportTicketRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Ticket not found"));
		supportTicketRepository.delete(ticket);
	}

	public void respondToTicket(Long ticketId, String response) {
		if (ticketId == null) {
			throw new IllegalArgumentException("Ticket ID is required");
		}
		if (response == null || response.trim().isEmpty()) {
			throw new IllegalArgumentException("Response is required");
		}
		
		SupportTicket ticket = supportTicketRepository.findById(ticketId)
				.orElseThrow(() -> new RuntimeException("Ticket not found"));
		ticket.setStatus(SupportTicket.Status.RESOLVED);
		ticket.setIssueDescription(ticket.getIssueDescription() + "\nResponse: " + response.trim());
		supportTicketRepository.save(ticket);
	}

	private SupportTicketDTO convertToDTO(SupportTicket ticket) {
		SupportTicketDTO dto = new SupportTicketDTO();
		dto.setId(ticket.getId());
		dto.setIssueTitle(ticket.getIssueTitle());
		dto.setIssueDescription(ticket.getIssueDescription());
		dto.setStatus(ticket.getStatus().name());
		dto.setCreatedDate(ticket.getCreatedDate());
		dto.setUserId(ticket.getUser() != null ? ticket.getUser().getId() : null);
		return dto;
	}
}