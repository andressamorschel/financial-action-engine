package com.financialactionengine.infrastructure.persistence;

import com.financialactionengine.domain.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    Optional<Action> findByClientIdAndIdempotencyKey(
            String clientId,
            String idempotencyKey
    );
}
