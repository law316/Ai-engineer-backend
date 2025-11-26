package net.ikwa.aiengineerpractice.repo;

import net.ikwa.aiengineerpractice.model.RagModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RagRepository extends JpaRepository<RagModel, Integer> {

    // ✅ Insert product + embedding (pgvector)
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO rag_model (product_name, product_description, product_price, embedding)
        VALUES (?1, ?2, ?3, cast(?4 AS vector))
    """, nativeQuery = true)
    void insertWithEmbedding(String name,
                             String description,
                             BigDecimal price,
                             String embeddingVector);

    // ✅ Similarity search using pgvector
    @Query(value = """
        SELECT * FROM rag_model
        ORDER BY embedding <=> cast(?1 AS vector)
        LIMIT 5
    """, nativeQuery = true)
    List<RagModel> searchByEmbedding(String embeddingVector);
}
