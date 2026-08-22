package org.dromara.ai.model.infrastructure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai-platform.security")
public class ModelSecurityProperties {
    private String encryptionKey;
    private String encryptionKeyVersion = "v1";
    private Map<String, String> decryptionKeys = new HashMap<>();
}
