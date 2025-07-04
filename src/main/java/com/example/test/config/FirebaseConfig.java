package com.example.test.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

//@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            // Đọc đường dẫn từ biến môi trường FIREBASE_CREDENTIAL_PATH
            String firebasePath = System.getenv("FIREBASE_CREDENTIAL_PATH");

            if (firebasePath == null || firebasePath.isEmpty()) {
                throw new RuntimeException("FIREBASE_CREDENTIAL_PATH environment variable is not set");
            }

            FileInputStream serviceAccount = new FileInputStream(firebasePath);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase Initialized from " + firebasePath);
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
