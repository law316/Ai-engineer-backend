package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.repo.RagRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class RagService {

    @Autowired
    private RagRepository ragRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Transactional
    public RagModel createProduct(RagModelDTO dto) {

        // Validate fields
        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty() ||
                dto.getProductDescription() == null || dto.getProductDescription().trim().isEmpty() ||
                dto.getProductPrice() <= 0)
        {
            throw new IllegalArgumentException("All fields are required");
        }

        // Combine fields for embedding
        String textToEmbed = dto.getProductName() + " " + dto.getProductDescription();

        // Generate a clean 1536-dim float[] embedding
        float[] vector = embeddingModel.embed(textToEmbed);

        // Create entity
        RagModel model = new RagModel();
        model.setProductName(dto.getProductName());
        model.setProductDescription(dto.getProductDescription());
        model.setProductPrice(dto.getProductPrice());
        model.setEmbedding(vector);

        return ragRepository.save(model);
    }

    // ------------------------
    // Semantic Search (RAG)
    // ------------------------
    public List<RagModel> search(String query) {

        float[] vector = embeddingModel.embed(query);

        // Convert float[] → '{0.2, 0.55, ...}'
        String vectorString = Arrays.toString(vector)
                .replace("[", "{")
                .replace("]", "}");

        return ragRepository.searchByEmbedding(vectorString);
    }
}
