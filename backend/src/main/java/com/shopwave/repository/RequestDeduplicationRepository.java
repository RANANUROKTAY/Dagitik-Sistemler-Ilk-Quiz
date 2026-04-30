package com.shopwave.repository;

import com.shopwave.domain.RequestDeduplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestDeduplicationRepository extends JpaRepository<RequestDeduplication, String> {
    // ID bazlı arama (findById) varsayılan olarak gelir
}