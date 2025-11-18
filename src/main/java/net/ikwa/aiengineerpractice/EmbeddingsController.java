package net.ikwa.aiengineerpractice;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EmbeddingsController {
    @Autowired

    private EmbeddingModel model;

    @RequestMapping("/embed")
    public float[] userEmbeds(@RequestBody String messsage) {
        return model.embed(messsage);
    }
}
