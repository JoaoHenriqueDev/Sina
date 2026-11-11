package br.ia.sina.api.notification.push;

import br.ia.sina.api.config.FirebaseProperties;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PushSender {

    private static final Logger log = LoggerFactory.getLogger(PushSender.class);
    private static final String APP_NAME = "sina";
    private static final Duration TTL = Duration.ofHours(24);
    private static final Set<MessagingErrorCode> INVALID_TOKEN =
            EnumSet.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH);

    private final FirebaseProperties properties;
    private volatile FirebaseMessaging messaging;

    public PushSender(FirebaseProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        String path = properties.credentialsPath();
        return path != null && !path.isBlank() && Files.isReadable(Path.of(path));
    }

    public PushResult send(String token, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(token)
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setTtl(TTL.toMillis())
                        .build())
                .build();
        try {
            messaging().send(message);
            return PushResult.SENT;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (INVALID_TOKEN.contains(code)) {
                return PushResult.INVALID_TOKEN;
            }
            log.warn("Falha no envio do push: {}", code);
            return PushResult.FAILED;
        }
    }

    private FirebaseMessaging messaging() {
        if (messaging == null) {
            synchronized (this) {
                if (messaging == null) {
                    messaging = FirebaseMessaging.getInstance(app());
                }
            }
        }
        return messaging;
    }

    private FirebaseApp app() {
        return FirebaseApp.getApps().stream()
                .filter(a -> a.getName().equals(APP_NAME))
                .findFirst()
                .orElseGet(() -> {
                    try (InputStream in = Files.newInputStream(Path.of(properties.credentialsPath()))) {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(in))
                                .setJsonFactory(GsonFactory.getDefaultInstance())
                                .build();
                        return FirebaseApp.initializeApp(options, APP_NAME);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Credencial do Firebase ilegível", e);
                    }
                });
    }
}
