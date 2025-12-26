package net.ikwa.aiengineerpractice.service;

import net.ikwa.aiengineerpractice.dto.RagModelDTO;
import net.ikwa.aiengineerpractice.model.RagModel;
import net.ikwa.aiengineerpractice.repo.RagRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ChatClient chatClient;

    @Autowired
    public RagService(RagRepository ragRepository,
                      EmbeddingModel embeddingModel,
                      ChatClient.Builder chatClientBuilder) {
        this.ragRepository = ragRepository;
        this.embeddingModel = embeddingModel;
        this.chatClient = chatClientBuilder.build();
    }

    // ✅ Convert float[] -> "[0.12,0.34,...]" for pgvector
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

    // ✅ Create + store product with embedding (UNCHANGED)
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

    // ✅ NEW: ingest an uploaded document WITHOUT touching createProduct()
    @Transactional
    public Map<String, Object> ingestDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalName = file.getOriginalFilename();
        String safeName = (originalName == null || originalName.isBlank())
                ? "Uploaded document"
                : originalName;

        // OLD SIMPLE LOGIC THAT WORKED
        String text = extractTextFromFile(file);

        String textToEmbed = safeName + " " + text;

        float[] vector = embeddingModel.embed(textToEmbed);
        String embeddingVector = toPgVectorString(vector);

        BigDecimal price = BigDecimal.ONE; // document marker

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


    // ✅ Text extraction (Tika / PDF support handled here)
    private String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase()
                : "";

        // Plain text
        if (contentType.contains("text") || filename.endsWith(".txt") || filename.endsWith(".md")) {
            return new String(file.getBytes());
        }

        // PDF (OLD UNLIMITED VERSION THAT WORKED)
        if (contentType.contains("pdf") || filename.endsWith(".pdf")) {
            try {
                org.apache.tika.parser.AutoDetectParser parser =
                        new org.apache.tika.parser.AutoDetectParser();
                org.apache.tika.metadata.Metadata metadata =
                        new org.apache.tika.metadata.Metadata();
                org.apache.tika.sax.BodyContentHandler handler =
                        new org.apache.tika.sax.BodyContentHandler(-1);

                try (java.io.InputStream stream = file.getInputStream()) {
                    parser.parse(
                            stream,
                            handler,
                            metadata,
                            new org.apache.tika.parser.ParseContext()
                    );
                }

                return handler.toString();
            } catch (Exception e) {
                throw new IOException("Failed to extract text from PDF", e);
            }
        }

        return new String(file.getBytes());
    }


    // ✅ Search by query text (UNCHANGED)
    public List<RagModel> search(String query) {
        float[] vector = embeddingModel.embed(query);
        String embeddingVector = toPgVectorString(vector);
        return ragRepository.searchByEmbedding(embeddingVector);
    }

    // 🔍 Helper: detect "document-style" entries (CVs, PDFs, knowledge docs)
    private boolean isDocumentStyleEntry(RagModel p) {
        String name = p.getProductName() != null ? p.getProductName() : "";
        String lowerName = name.toLowerCase();

        boolean looksLikeFile =
                lowerName.endsWith(".pdf") ||
                        lowerName.endsWith(".doc") ||
                        lowerName.endsWith(".docx");

        // Our ingestDocument uses price = 1 for docs
        boolean hasTinyPrice = false;
        if (p.getProductPrice() != null) {
            hasTinyPrice = p.getProductPrice().compareTo(BigDecimal.valueOf(5)) <= 0;
        }

        return looksLikeFile || hasTinyPrice;
    }

    public String makeNaturalResponse(String query, List<RagModel> results) {
        if (results == null || results.isEmpty()) {
            return "I couldn’t find any clear match for \"" + query + "\" right now.";
        }

        String qLower = query.toLowerCase();

        // Is the user likely asking for a profile / CV / document?
        boolean askingForProfile =
                qLower.contains("cv") ||
                        qLower.contains("resume") ||
                        qLower.contains("profile") ||
                        qLower.contains("about joshua") ||
                        qLower.contains("about me") ||
                        qLower.contains("who is");

        // 1) Filter results depending on query type
        List<RagModel> filtered = new ArrayList<>();
        for (RagModel p : results) {
            boolean docStyle = isDocumentStyleEntry(p);

            if (askingForProfile) {
                // For profile/document questions: prefer document-style entries
                if (docStyle) {
                    filtered.add(p);
                }
            } else {
                // For normal questions (pricing, products, etc.): ignore document-style CV rows
                if (!docStyle) {
                    filtered.add(p);
                }
            }

            if (filtered.size() >= 3) {
                break; // cap context to 3 items
            }
        }

        // If the filter knocked out everything, fall back to the original top 3,
        // but still avoid dumping huge descriptions.
        if (filtered.isEmpty()) {
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                filtered.add(results.get(i));
            }
        }

        try {
            // 2) Build concise context (no full CV dump)
            StringBuilder context = new StringBuilder();
            for (RagModel p : filtered) {
                String name = p.getProductName();
                String desc = p.getProductDescription();
                BigDecimal price = p.getProductPrice();

                if (desc != null && desc.length() > 400) {
                    desc = desc.substring(0, 400) + "...";
                }

                context.append("Item: ").append(name == null ? "" : name)
                        .append("\nSummary: ").append(desc == null ? "" : desc)
                        .append("\nPrice: ").append(price == null ? "" : price.toPlainString())
                        .append("\n\n");
            }

            // 3) Strong instructions to the model
            return chatClient
                    .prompt()
                    .system("""
                           You are a professional medical support advisor (pharmacist / healthcare personnel style).
                            
                                INTRODUCTION & GREETINGS:
                                - When the user says “hi”, “hello”, or greets you, respond warmly.
                                - Use appropriate greetings like:
                                  • “Good morning”
                                  • “Good afternoon”
                                  • “Good evening”
                                  • “Hello, how are you doing?”
                                - After greeting, briefly state your role, e.g.:
                                  “I’m here to support you with health-related questions and guidance.”
                                - Make it clear you provide medical support and advice, and that you will refer to a hospital when necessary.
                            
                                IMPORTANT ROLE LIMITS:
                                - You are NOT a doctor.
                                - You must NEVER give a final medical diagnosis.
                                - You must NEVER claim to replace a hospital or physician.
                            
                                Your role:
                                - Listen carefully to the user’s health complaint.
                                - Respond like a calm, experienced real-life medical personnel.
                                - Provide safe, practical medical advice and drug guidance.
                                - Explain clearly, professionally, and with empathy.
                                - If symptoms appear serious, worsening, unusual, or risky, clearly advise the user to visit a hospital or doctor.
                            
                                Language & tone rules:
                                - Always reply in the SAME language the user uses.
                                - If the user uses Nigerian Pidgin, reply in clear and respectful Pidgin.
                                - If the user uses English, reply in friendly professional English.
                                - Be polite, reassuring, and human — like a real healthcare worker.
                            
                                Medical safety rules:
                                - Do NOT diagnose conditions.
                                - Do NOT invent diseases.
                                - You MAY suggest common medications and general usage guidance where appropriate.
                                - Always include safety advice (e.g. dosage caution, take after food if needed).
                                - Ask at most 1–2 short follow-up questions only when necessary.
                                - If symptoms involve severe pain, bleeding, breathing difficulty, pregnancy, children, high fever, or long duration → clearly refer to a hospital immediately.
                            
                                How to use the provided knowledge:
                                - Use ONLY medically relevant information from the provided context.
                                - Summarize in simple, human language.
                                - Do NOT dump long documents.
                                - Connect advice naturally to the user’s symptoms.
                        """)
                    .user("User query: " + query + "\n\nRelevant items:\n" + context)
                    .call()
                    .content();

        } catch (Exception e) {
            e.printStackTrace();

            // 4) Fallback: short manual summary using the filtered list
            StringBuilder summary = new StringBuilder();
            summary.append("Here are some options I found for \"")
                    .append(query)
                    .append("\": ");

            int limit = Math.min(filtered.size(), 3);
            for (int i = 0; i < limit; i++) {
                RagModel p = filtered.get(i);
                if (i > 0) summary.append("; ");
                summary.append(p.getProductName());

                if (p.getProductPrice() != null) {
                    summary.append(" (around $")
                            .append(p.getProductPrice().toPlainString())
                            .append(")");
                }
            }

            if (filtered.size() > limit) {
                summary.append(" and more.");
            }

            return summary.toString();
        }
    }
    public long countTrainingDocuments() {
        return ragRepository.countTrainingDocuments();
    }
    public List<RagModel> getRecentTrainingDocs() {
        return ragRepository.findRecentTrainingDocs();
    }
    private List<String> chunkText(String text) {
        int CHUNK_SIZE = 1800;   // safe
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
