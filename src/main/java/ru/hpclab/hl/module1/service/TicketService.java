package ru.hpclab.hl.module1.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.hpclab.hl.module1.repository.TicketRepository;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;

    public Long getMaxViewersByDay(Long movieId, LocalDate date) {
        return ticketRepository.countTicketsByMovieAndDate(movieId, date);
    }

}