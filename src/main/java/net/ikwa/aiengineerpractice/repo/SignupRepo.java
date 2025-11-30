package net.ikwa.aiengineerpractice.repo;

import net.ikwa.aiengineerpractice.model.SignupModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface SignupRepo extends JpaRepository<SignupModel, Integer> {
    Optional<SignupModel> findByUsername(String userName);
    Optional<SignupModel> findByPhoneNumber(String phoneNumber);
}
