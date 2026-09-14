package com.petramas.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    @PostMapping("/leer-texto")
    public ResponseEntity<List<String>> extraerTexto(@RequestParam("file") MultipartFile file) {
        List<String> resultadosTextos = new ArrayList<>();

        try {
            // Obtenemos el JSON de la credencial desde la variable de entorno de Render
            String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
            
            if (credentialsJson == null || credentialsJson.isEmpty()) {
                resultadosTextos.add("Error: La variable de entorno GOOGLE_CREDENTIALS_JSON no está configurada.");
                return ResponseEntity.internalServerError().body(resultadosTextos);
            }

            InputStream credentialsStream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            // Convertir la imagen subida a bytes
            byte[] imageBytes = file.getBytes();
            ByteString imgBytes = ByteString.copyFrom(imageBytes);

            List<AnnotateImageRequest> requests = new ArrayList<>();
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
            
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();
            requests.add(request);

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        resultadosTextos.add("Error de Vision API: " + res.getError().getMessage());
                        return ResponseEntity.badRequest().body(resultadosTextos);
                    }
                    resultadosTextos.add(res.getFullTextAnnotation().getText());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            resultadosTextos.add("Error detallado: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resultadosTextos);
        }

        return ResponseEntity.ok(resultadosTextos);
    }
}
