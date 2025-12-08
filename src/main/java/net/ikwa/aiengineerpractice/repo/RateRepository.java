// src/main/java/net/ikwa/aiengineerpractice/repo/RateRepository.java
package net.ikwa.aiengineerpractice.repo;

import net.ikwa.aiengineerpractice.model.RateModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateRepository extends JpaRepository<RateModel, Integer> {

    // used to get the latest rate snapshot
    RateModel findTopByOrderByUpdatedAtDesc();

    // optional history list for dashboard
    List<RateModel> findAllByOrderByUpdatedAtDesc();
}
