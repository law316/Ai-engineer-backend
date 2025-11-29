package net.ikwa.aiengineerpractice.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private final ChatClient chatClient;
    private final OpenAiImageModel openAiImageModel;

    public ImageController(ChatClient.Builder builder, OpenAiImageModel openAiImageModel) {
        this.chatClient = builder.build();
        this.openAiImageModel = openAiImageModel;
    }

    // ✅ 1) Generate image from text prompt
    @PostMapping("/niceimage")
    public ResponseEntity<String> genImage(@RequestBody String prompt) {
        try {
            OpenAiImageOptions options = OpenAiImageOptions.builder()
                    .quality("hd")
                    .width(1024)
                    .height(1024)
                    .build();

            ImagePrompt imagePrompt = new ImagePrompt(prompt, options);

            ImageResponse imageResponse = openAiImageModel.call(imagePrompt);

            // ✅ Get the generated image URL
            String url = imageResponse.getResult().getOutput().getUrl();

            // ✅ Return the URL as plain text
            return ResponseEntity.ok(url);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error generating image");
        }
    }


    // ✅ 2) Upload image + text and get description
    @PostMapping(
            value = "/imageupload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> describeImage(
            @RequestParam("describe") String describe,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Determine mime type (fallback to JPEG)
            String contentType = file.getContentType();
            MimeType mimeType;
            if (contentType != null) {
                mimeType = MimeType.valueOf(contentType);
            } else {
                mimeType = MimeTypeUtils.IMAGE_JPEG;
            }

            String description = chatClient
                    .prompt()
                    .user(userSpec -> userSpec
                            .text(describe)
                            .media(mimeType, file.getResource()))
                    .call()
                    .content();

            // 🔥 Return the actual description to the frontend
            return ResponseEntity.ok(description);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("error describing image");
        }
    }
}
