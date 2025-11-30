package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.model.SignupModel;
import net.ikwa.aiengineerpractice.repo.SignupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SignupService {
      @Autowired
    private SignupRepo signupRepo;

    public void signupUser(SignupModel signupModel) {
        if (signupModel.getPhoneNumber() == null || signupModel.getPhoneNumber().isEmpty()
        || signupModel.getUsername() == null || signupModel.getUsername().isEmpty()) {
            throw new IllegalArgumentException("all details must be filled");
        }
        signupRepo.save(signupModel);
    }
}
