package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.repo.RagRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RagService {

    @Autowired
    private RagRepository ragRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    // ✅ Convert float[] -> "[0.12, 0.34, ...]" for pgvector
    private String toPgVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(","); // no space needed, pgvector accepts this
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    // ✅ Create + store product with embedding
    @Transactional
    public void createProduct(RagModelDTO dto) {

        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty() ||
                dto.getProductDescription() == null || dto.getProductDescription().trim().isEmpty() ||
                dto.getProductPrice() <= 0) {
            throw new IllegalArgumentException("All fields are required and price must be > 0");
        }

        // Text to embed
        String textToEmbed = dto.getProductName() + " " + dto.getProductDescription();

        // 1. Get embedding from Spring AI
        float[] vector = embeddingModel.embed(textToEmbed);

        // 2. Convert to pgvector-string
        String embeddingVector = toPgVectorString(vector);

        // 3. Insert via native SQL
        BigDecimal price = BigDecimal.valueOf(dto.getProductPrice());

        ragRepository.insertWithEmbedding(
                dto.getProductName(),
                dto.getProductDescription(),
                price,
                embeddingVector
        );
    }

    // ✅ Search by query text
    public List<RagModel> search(String query) {

        // 1. Embed search text
        float[] vector = embeddingModel.embed(query);

        // 2. Convert to pgvector-string
        String embeddingVector = toPgVectorString(vector);

        // 3. Similarity search
        return ragRepository.searchByEmbedding(embeddingVector);
    }
}
