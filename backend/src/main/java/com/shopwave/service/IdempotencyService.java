package com.shopwave.service;

import com.shopwave.domain.RequestDeduplication;
import com.shopwave.repository.RequestDeduplicationRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {
    private final RequestDeduplicationRepository repository;

    public IdempotencyService(RequestDeduplicationRepository repository) {
        this.repository = repository;
    }

    public Optional<RequestDeduplication> findRequest(String requestId) {
        return repository.findById(requestId);
    }

    public void saveResponse(String requestId, int status, String body) {
        RequestDeduplication entry = new RequestDeduplication();
        entry.setRequestId(requestId);
        entry.setResponseStatus(status);
        entry.setResponseBody(body);
        entry.setCreatedAt(LocalDateTime.now());
        repository.save(entry);
    }
}