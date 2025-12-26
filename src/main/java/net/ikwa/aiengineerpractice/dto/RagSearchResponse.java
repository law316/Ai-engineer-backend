package net.ikwa.aiengineerpractice.dto;

import net.ikwa.aiengineerpractice.model.RagModel;
import java.util.List;

public class RagSearchResponse {

    private List<RagModel> results;
    private String aiResponse;

    public RagSearchResponse(List<RagModel> results, String aiResponse) {
        this.results = results;
        this.aiResponse = aiResponse;
    }

    public List<RagModel> getResults() {
        return results;
    }

    public void setResults(List<RagModel> results) {
        this.results = results;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
}
