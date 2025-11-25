package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.repo.RagRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private RagRepository ragRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Transactional
    public RagModel createProduct(RagModelDTO dto) {

        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty() ||
                dto.getProductDescription() == null || dto.getProductDescription().trim().isEmpty() ||
                dto.getProductPrice() <= 0) {
            throw new IllegalArgumentException("All fields are required");
        }

        String textToEmbed = dto.getProductName() + " " + dto.getProductDescription();

        float[] vector = embeddingModel.embed(textToEmbed);

        List<Float> embeddingList = new ArrayList<>();

        for (float v : vector) {
            embeddingList.add(v);
        }
        RagModel model = new RagModel();
        model.setProductName(dto.getProductName());
        model.setProductDescription(dto.getProductDescription());
        model.setProductPrice(dto.getProductPrice());
        model.setEmbedding(embeddingList);

        return ragRepository.save(model);
    }

    public List<RagModel> search(String query) {

        float[] vector = embeddingModel.embed(query);

        // Convert float[] → '{1.23, 4.56, ...}'
        String vectorString = Arrays.toString(vector)
                .replace("[", "{")
                .replace("]", "}");

        return ragRepository.searchByEmbedding(vectorString);
    }
}
