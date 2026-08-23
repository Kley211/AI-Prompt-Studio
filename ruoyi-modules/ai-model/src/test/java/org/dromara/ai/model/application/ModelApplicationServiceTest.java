package org.dromara.ai.model.application;

import org.dromara.ai.common.error.AiPlatformException;
import org.dromara.ai.model.domain.AiModel;
import org.dromara.ai.model.domain.ModelCredential;
import org.dromara.ai.model.domain.ModelProvider;
import org.dromara.ai.model.domain.ModelStatus;
import org.dromara.ai.model.domain.ProjectModel;
import org.dromara.ai.model.infrastructure.AiModelMapper;
import org.dromara.ai.model.infrastructure.ModelCredentialMapper;
import org.dromara.ai.model.infrastructure.ModelProviderMapper;
import org.dromara.ai.model.infrastructure.ProjectModelMapper;
import org.dromara.ai.model.infrastructure.SecretCipher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class ModelApplicationServiceTest {
    private final ModelProviderMapper providerMapper = mock(ModelProviderMapper.class);
    private final AiModelMapper modelMapper = mock(AiModelMapper.class);
    private final ModelCredentialMapper credentialMapper = mock(ModelCredentialMapper.class);
    private final ProjectModelMapper projectModelMapper = mock(ProjectModelMapper.class);
    private final SecretCipher secretCipher = mock(SecretCipher.class);
    private final ModelApplicationService service = new ModelApplicationService(providerMapper, modelMapper,
        credentialMapper, projectModelMapper, secretCipher, JsonMapper.builder().build());

    @Test
    void credentialResponseNeverContainsEncryptedOrPlainSecret() {
        ModelProvider provider = activeProvider();
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(credentialMapper.selectOne(any())).thenReturn(null);
        when(secretCipher.encrypt("sk-sensitive-value"))
            .thenReturn(new SecretCipher.EncryptedSecret("ciphertext", "v1"));

        ModelCredentialView view = service.saveCredential(1L,
            new ModelCommand.SaveCredential("default", "sk-sensitive-value"));

        assertEquals("sk-sensi", view.secretPrefix());
        assertFalse(view.toString().contains("ciphertext"));
        assertFalse(view.toString().contains("sk-sensitive-value"));
        verify(credentialMapper).insert(any(ModelCredential.class));
    }

    @Test
    void runtimeConfigRequiresProjectGrantAndDecryptsOnlySelectedCredential() {
        ProjectModel grant = new ProjectModel();
        grant.setProjectId(10L);
        grant.setModelId(20L);
        grant.setStatus(ModelStatus.ACTIVE);
        AiModel model = new AiModel();
        model.setId(20L);
        model.setProviderId(1L);
        model.setCode("gpt-test");
        model.setStatus(ModelStatus.ACTIVE);
        ModelCredential credential = new ModelCredential();
        credential.setProviderId(1L);
        credential.setEncryptedSecret("ciphertext");
        credential.setKeyVersion("v1");
        credential.setStatus(ModelStatus.ACTIVE);
        when(projectModelMapper.selectOne(any())).thenReturn(grant);
        when(modelMapper.selectById(20L)).thenReturn(model);
        when(providerMapper.selectById(1L)).thenReturn(activeProvider());
        when(credentialMapper.selectList(any())).thenReturn(List.of(credential));
        when(secretCipher.decrypt("ciphertext", "v1")).thenReturn("sk-runtime");

        ModelRuntimeConfig runtime = service.runtimeConfig(10L, 20L);

        assertEquals("gpt-test", runtime.modelCode());
        assertEquals("sk-runtime", runtime.apiKey());
        verify(secretCipher).decrypt("ciphertext", "v1");
    }

    @Test
    void disabledModelCannotBeGrantedToProject() {
        AiModel model = new AiModel();
        model.setId(20L);
        model.setProviderId(1L);
        model.setStatus(ModelStatus.DISABLED);
        when(modelMapper.selectById(20L)).thenReturn(model);

        assertThrows(AiPlatformException.class, () -> service.grantProjectModel(10L,
            new ModelCommand.GrantProjectModel(20L, "default")));
    }

    private ModelProvider activeProvider() {
        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setName("OpenAI Compatible");
        provider.setBaseUrl("https://example.com/v1");
        provider.setStatus(ModelStatus.ACTIVE);
        return provider;
    }
}
