package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.model.SignupModel;
import net.ikwa.aiengineerpractice.repo.SignupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    @Autowired
    private SignupRepo signupRepo;

    public SignupModel authenticateUser(SignupModel user) {

        if (user == null) {
            throw new IllegalArgumentException("Request body is null");
        }

        // We ONLY care about phone number for login
        String phoneNumber = user.getPhoneNumber();

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new IllegalArgumentException("Phone number is null or empty");
        }

        // Find from DB by phone number
        Optional<SignupModel> findUser = signupRepo.findByPhoneNumber(phoneNumber);

        if (findUser.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        SignupModel storedUser = findUser.get();

        // Extra safety check
        if (!phoneNumber.equals(storedUser.getPhoneNumber())) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        // ✅ username from DB is used, no need to validate input username
        return storedUser;
    }
}
