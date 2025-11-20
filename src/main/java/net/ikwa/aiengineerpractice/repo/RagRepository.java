package net.ikwa.aiengineerpractice.repo;

import net.ikwa.aiengineerpractice.model.RagModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagRepository extends JpaRepository<RagModel, Integer> {
    @Query(value = """
        SELECT * FROM rag_products
        ORDER BY embedding <=> cast(?1 AS vector)
        LIMIT 5
    """, nativeQuery = true)
    List<RagModel> searchByEmbedding(String embeddingVector);
}
