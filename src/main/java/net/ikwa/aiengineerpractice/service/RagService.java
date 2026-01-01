package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.repo.RagRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private final RagRepository ragRepository;
    private final EmbeddingModel embeddingModel;
    private final ChatClient ragChatClient;
    private final ChatClient ragVisionChatClient;

    @Autowired
    public RagService(
            RagRepository ragRepository,
            EmbeddingModel embeddingModel,
            @Qualifier("ragChatClient") ChatClient ragChatClient,
            @Qualifier("ragVisionChatClient") ChatClient ragVisionChatClient) {
        this.ragRepository = ragRepository;
        this.embeddingModel = embeddingModel;
        this.ragChatClient = ragChatClient;
        this.ragVisionChatClient = ragVisionChatClient;
    }

    // ... rest of your methods



    // Convert float[] -> "[0.12,0.34,...]" for pgvector
    private String toPgVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    // Create + store product with embedding
    @Transactional
    public void createProduct(RagModelDTO dto) {
        if (dto.getProductName() == null || dto.getProductName().trim().isEmpty() ||
                dto.getProductDescription() == null || dto.getProductDescription().trim().isEmpty() ||
                dto.getProductPrice() <= 0) {
            throw new IllegalArgumentException("All fields are required and price must be > 0");
        }

        String textToEmbed = dto.getProductName() + " " + dto.getProductDescription();
        float[] vector = embeddingModel.embed(textToEmbed);
        String embeddingVector = toPgVectorString(vector);
        BigDecimal price = BigDecimal.valueOf(dto.getProductPrice());

        ragRepository.insertWithEmbedding(
                dto.getProductName(),
                dto.getProductDescription(),
                price,
                embeddingVector
        );
    }

    // Ingest uploaded document
    @Transactional
    public Map<String, Object> ingestDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalName = file.getOriginalFilename();
        String safeName = (originalName == null || originalName.isBlank()) ? "Uploaded document" : originalName;

        String text = extractTextFromFile(file);
        String textToEmbed = safeName + " " + text;
        float[] vector = embeddingModel.embed(textToEmbed);
        String embeddingVector = toPgVectorString(vector);
        BigDecimal price = BigDecimal.ONE; // marker for docs

        ragRepository.insertWithEmbedding(
                safeName,
                text,
                price,
                embeddingVector
        );

        Map<String, Object> response = new HashMap<>();
        response.put("name", safeName);
        response.put("size", file.getSize());
        response.put("type", file.getContentType());
        response.put("status", "ready");
        response.put("uploadedAt", Instant.now().toString());

        return response;
    }

    // Extract text from file
    private String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        // Plain text
        if (contentType.contains("text") || filename.endsWith(".txt") || filename.endsWith(".md")) {
            return new String(file.getBytes());
        }

        // PDF
        if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
            try {
                org.apache.tika.parser.AutoDetectParser parser = new org.apache.tika.parser.AutoDetectParser();
                org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
                org.apache.tika.sax.BodyContentHandler handler = new org.apache.tika.sax.BodyContentHandler(-1);

                try (java.io.InputStream stream = file.getInputStream()) {
                    parser.parse(stream, handler, metadata, new org.apache.tika.parser.ParseContext());
                }
                return handler.toString();
            } catch (Exception e) {
                throw new IOException("Failed to extract text from PDF", e);
            }
        }

        return new String(file.getBytes());
    }

    // Search by query text
    public List<RagModel> search(String query) {
        float[] vector = embeddingModel.embed(query);
        String embeddingVector = toPgVectorString(vector);
        return ragRepository.searchByEmbedding(embeddingVector);
    }

    // Detect document-style entries (PDF, CV, knowledge docs)
    private boolean isDocumentStyleEntry(RagModel p) {
        String name = p.getProductName() != null ? p.getProductName() : "";
        String lowerName = name.toLowerCase();

        boolean looksLikeFile = lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx");
        boolean hasTinyPrice = p.getProductPrice() != null && p.getProductPrice().compareTo(BigDecimal.valueOf(5)) <= 0;

        return looksLikeFile || hasTinyPrice;
    }

    // Generate AI natural response
    public String makeNaturalResponse(String query, List<RagModel> results, String conversationId) {
        String qLower = query.toLowerCase().trim();

        // ✅ HANDLE GREETINGS WITHOUT RAG CONTEXT
        if (isGreeting(qLower)) {
            return handleGreeting(qLower, conversationId);
        }

        // ✅ HANDLE EMPTY RESULTS BETTER
        if (results == null || results.isEmpty()) {
            return "I'm here to help with your health questions. Could you please describe your symptoms or tell me what medication information you need?";
        }

        // Detect query type
        boolean askingForProfile = qLower.contains("cv") ||
                qLower.contains("resume") ||
                qLower.contains("profile") ||
                qLower.contains("about joshua") ||
                qLower.contains("about me") ||
                qLower.contains("who is");

        // Filter results
        List<RagModel> filtered = new ArrayList<>();
        for (RagModel p : results) {
            boolean docStyle = isDocumentStyleEntry(p);
            if ((askingForProfile && docStyle) || (!askingForProfile && !docStyle)) {
                filtered.add(p);
            }
            if (filtered.size() >= 5) break;  // ✅ Increased from 3 to 5
        }

        if (filtered.isEmpty()) {
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                filtered.add(results.get(i));
            }
        }

        try {
            // ✅ BUILD RICHER CONTEXT
            StringBuilder context = new StringBuilder();
            for (RagModel p : filtered) {
                String name = p.getProductName();
                String desc = p.getProductDescription();
                BigDecimal price = p.getProductPrice();

                // ✅ Increased context length
                if (desc != null && desc.length() > 800) {
                    desc = desc.substring(0, 800) + "...";
                }

                context.append("===\n")
                        .append("Item: ").append(name == null ? "" : name).append("\n")
                        .append("Details: ").append(desc == null ? "" : desc).append("\n")
                        .append("Price: $").append(price == null ? "N/A" : price.toPlainString())
                        .append("\n===\n\n");
            }

            return ragChatClient
                    .prompt()
                    .advisors(advisor -> advisor.param("conversationId", conversationId))
                    .system("""
                       You are a professional medical support advisor (pharmacist / healthcare personnel).
                       
                       INTRODUCTION & GREETINGS:
                       - Respond warmly to greetings.
                       - Use appropriate time-based greetings (Good morning/afternoon/evening).
                       - After greeting, briefly state your role.
                       
                       IMPORTANT ROLE LIMITS:
                       - You are NOT a doctor.
                       - Never give final diagnoses.
                       - Always clarify you provide support, not replace physicians.
                       
                       YOUR RESPONSIBILITIES:
                       - Listen carefully to health complaints.
                       - Provide safe, practical medical advice.
                       - Explain medication guidance clearly.
                       - Use empathy and professionalism.
                       - Refer to hospitals for severe symptoms.
                       
                       RESPONSE STYLE:
                       - Reply in the user's language.
                       - Be conversational and human.
                       - Use bullet points for clarity when listing information.
                       - Provide 2-4 paragraph responses minimum.
                       - Ask 1-2 follow-up questions when appropriate.
                       
                       MEDICAL SAFETY:
                       - Do NOT diagnose conditions definitively.
                       - Suggest OTC medications when appropriate.
                       - Always mention "consult a doctor if symptoms persist or worsen."
                       
                       KNOWLEDGE BASE USAGE:
                       - Use ONLY medically relevant information from the provided context.
                       - Summarize in simple, human language.
                       - Don't dump raw document text.
                       - If context is sparse, acknowledge it and provide general guidance.
                """)
                    .user("User query: " + query + "\n\nRelevant medical knowledge:\n" + context)
                    .call()
                    .content();

        } catch (Exception e) {
            System.err.println("❌ ChatClient error: " + e.getMessage());
            e.printStackTrace();

            // ✅ Better fallback response
            return buildFallbackResponse(query, filtered);
        }
    }

    // ✅ NEW: Greeting detection
    private boolean isGreeting(String query) {
        return query.matches(".*(hi|hello|hey|good morning|good afternoon|good evening|greetings).*");
    }

    // ✅ NEW: Greeting handler
    private String handleGreeting(String query, String conversationId) {
        String timeOfDay = getTimeOfDay();

        return ragChatClient
                .prompt()
                .advisors(advisor -> advisor.param("conversationId", conversationId))
                .system("""
                   You are a friendly medical support advisor.
                   Respond warmly to the greeting.
                   Use the appropriate time-based greeting.
                   Briefly introduce yourself and offer help.
                   Keep it conversational and welcoming.
            """)
                .user("User said: " + query + "\n\nTime of day: " + timeOfDay)
                .call()
                .content();
    }

    // ✅ NEW: Time-based greeting
    private String getTimeOfDay() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "morning";
        if (hour < 17) return "afternoon";
        return "evening";
    }

    // ✅ IMPROVED: Fallback response
    private String buildFallbackResponse(String query, List<RagModel> filtered) {
        if (filtered.isEmpty()) {
            return "I'm here to assist with your health questions. Could you provide more details about your symptoms or the medication you're asking about?";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Based on your query about \"").append(query).append("\", here's what I found:\n\n");

        int limit = Math.min(filtered.size(), 3);
        for (int i = 0; i < limit; i++) {
            RagModel p = filtered.get(i);
            summary.append("• ").append(p.getProductName());
            if (p.getProductPrice() != null && p.getProductPrice().compareTo(BigDecimal.ONE) > 0) {
                summary.append(" ($").append(p.getProductPrice().toPlainString()).append(")");
            }
            summary.append("\n");
        }

        summary.append("\nWould you like more details about any of these?");
        return summary.toString();
    }



    public long countTrainingDocuments() {
        return ragRepository.countTrainingDocuments();
    }

    public List<RagModel> getRecentTrainingDocs() {
        return ragRepository.findRecentTrainingDocs();
    }

    private List<String> chunkText(String text) {
        int CHUNK_SIZE = 1800;
        int OVERLAP = 200;
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start = end - OVERLAP;
            if (start < 0) start = 0;
        }
        return chunks;
    }
}
