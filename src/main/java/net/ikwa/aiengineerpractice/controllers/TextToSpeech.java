package net.ikwa.aiengineerpractice.controllers;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
public class TextToSpeech {

    private final OpenAiAudioTranscriptionModel audioTranscription;

    public TextToSpeech(OpenAiAudioTranscriptionModel audioTranscription) {
        this.audioTranscription = audioTranscription;
    }

    @PostMapping("/speech-to-text")
    public ResponseEntity<?> speechToText(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body("No audio file uploaded or file is empty");
            }

            // Build transcription options (SRT subtitles as output)
            OpenAiAudioTranscriptionOptions options =
                    OpenAiAudioTranscriptionOptions.builder()
                            .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.SRT)
                            .build();

            // Create prompt using the uploaded file
            AudioTranscriptionPrompt prompt =
                    new AudioTranscriptionPrompt(file.getResource(), options);

            // Call the model
            AudioTranscriptionResponse response = audioTranscription.call(prompt);

            String transcript = response.getResult().getOutput();

            // ✅ Normal OK response
            return ResponseEntity.ok(transcript);

        } catch (Exception e) {
            e.printStackTrace();
            // ❌ Something went wrong → 500
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to transcribe audio: " + e.getMessage());
        }
    }
}
