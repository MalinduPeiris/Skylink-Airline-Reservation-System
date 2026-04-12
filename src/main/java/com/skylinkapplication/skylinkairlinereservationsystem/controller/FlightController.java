package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FlightDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/api/flight")
public class FlightController {

    @Autowired
    private FlightService flightService;

    @GetMapping("/search")
    public String searchFlights(@RequestParam(required = false) Date departureDate,
                                @RequestParam(required = false) String destination,
                                @RequestParam(required = false) Double maxPrice,
                                @RequestParam(required = false) String cabinClass,
                                Model model) {
        List<FlightDTO> flights = flightService.searchFlights(departureDate, destination, maxPrice, cabinClass);
        model.addAttribute("flights", flights);
        return "flight-search";
    }

    @GetMapping("/schedule")
    public String viewSchedule(Model model) {
        List<FlightDTO> flights = flightService.getAllFlights();
        model.addAttribute("flights", flights);
        return "schedule-management";
    }
}