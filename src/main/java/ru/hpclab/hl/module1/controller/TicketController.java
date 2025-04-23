package ru.hpclab.hl.module1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hpclab.hl.module1.dto.TicketDto;
import ru.hpclab.hl.module1.service.TicketService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ticket")
@RequiredArgsConstructor
@Tag(name = "Ticket Controller")
public class TicketController {

    private final TicketService ticketService;


    @GetMapping("/viewers/{movieId}")
    @Operation(summary = "Получение максимального количества зрителей на фильме за день")
    public Long getMaxMovieViewersByDay(
            @PathVariable Long movieId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ticketService.getMaxViewersByDay(movieId, date);
    }

}
