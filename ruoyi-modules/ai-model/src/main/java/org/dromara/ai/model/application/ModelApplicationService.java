package org.dromara.ai.model.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.common.error.AiErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;

/**
 * 模型治理模块公开应用服务。跨模块调用必须通过本服务，不得直接访问模型 Mapper。
 */
@Service
@RequiredArgsConstructor
public class ModelApplicationService {
    private static final int SECRET_PREFIX_LENGTH = 8;

    private final ModelProviderMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final ModelCredentialMapper credentialMapper;
    private final ProjectModelMapper projectModelMapper;
    private final SecretCipher secretCipher;
    private final JsonMapper jsonMapper;

    public List<ModelProvider> providers() {
        return providerMapper.selectList(Wrappers.<ModelProvider>lambdaQuery()
            .orderByAsc(ModelProvider::getName));
    }

    public ModelProvider provider(long providerId) {
        ModelProvider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw notFound();
        }
        return provider;
    }

    @Transactional
    public ModelProvider createProvider(ModelCommand.CreateProvider command) {
        requireUniqueProviderName(command.name(), null);
        ModelProvider provider = new ModelProvider();
        provider.setName(command.name().trim());
        provider.setProtocol(command.protocol());
        provider.setBaseUrl(normalizeBaseUrl(command.baseUrl()));
        provider.setDescription(command.description());
        provider.setStatus(ModelStatus.ACTIVE);
        providerMapper.insert(provider);
        return provider;
    }

    @Transactional
    public ModelProvider updateProvider(long providerId, ModelCommand.UpdateProvider command) {
        ModelProvider provider = provider(providerId);
        requireUniqueProviderName(command.name(), providerId);
        provider.setName(command.name().trim());
        provider.setBaseUrl(normalizeBaseUrl(command.baseUrl()));
        provider.setDescription(command.description());
        provider.setStatus(command.status());
        providerMapper.updateById(provider);
        return provider;
    }

    public List<ModelCredentialView> credentials(long providerId) {
        provider(providerId);
        return credentialMapper.selectList(Wrappers.<ModelCredential>lambdaQuery()
                .eq(ModelCredential::getProviderId, providerId)
                .orderByAsc(ModelCredential::getName))
            .stream().map(this::credentialView).toList();
    }

    @Transactional
    public ModelCredentialView saveCredential(long providerId, ModelCommand.SaveCredential command) {
        provider(providerId);
        ModelCredential credential = credentialMapper.selectOne(Wrappers.<ModelCredential>lambdaQuery()
            .eq(ModelCredential::getProviderId, providerId)
            .eq(ModelCredential::getName, command.name().trim()));
        SecretCipher.EncryptedSecret encrypted = secretCipher.encrypt(command.secret());
        if (credential == null) {
            credential = new ModelCredential();
            credential.setProviderId(providerId);
            credential.setName(command.name().trim());
        }
        credential.setSecretPrefix(secretPrefix(command.secret()));
        credential.setEncryptedSecret(encrypted.ciphertext());
        credential.setKeyVersion(encrypted.keyVersion());
        credential.setStatus(ModelStatus.ACTIVE);
        if (credential.getId() == null) {
            credentialMapper.insert(credential);
        } else {
            credentialMapper.updateById(credential);
        }
        return credentialView(credential);
    }

    @Transactional
    public void disableCredential(long credentialId) {
        ModelCredential credential = requireCredential(credentialId);
        credential.setStatus(ModelStatus.DISABLED);
        credentialMapper.updateById(credential);
    }

    public List<AiModel> models(Long providerId) {
        var query = Wrappers.<AiModel>lambdaQuery().orderByAsc(AiModel::getDisplayName);
        if (providerId != null) {
            query.eq(AiModel::getProviderId, providerId);
        }
        return modelMapper.selectList(query);
    }

    public AiModel model(long modelId) {
        AiModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw notFound();
        }
        return model;
    }

    @Transactional
    public AiModel createModel(ModelCommand.CreateModel command) {
        provider(command.providerId());
        requireUniqueModelCode(command.providerId(), command.code(), null);
        AiModel model = new AiModel();
        model.setProviderId(command.providerId());
        model.setCode(command.code().trim());
        model.setDisplayName(command.displayName().trim());
        model.setModelType(command.modelType());
        applyModelSettings(model, command.capabilities(), command.contextWindow(),
            command.inputPrice(), command.outputPrice(), ModelStatus.ACTIVE);
        modelMapper.insert(model);
        return model;
    }

    @Transactional
    public AiModel updateModel(long modelId, ModelCommand.UpdateModel command) {
        AiModel model = model(modelId);
        model.setDisplayName(command.displayName().trim());
        applyModelSettings(model, command.capabilities(), command.contextWindow(),
            command.inputPrice(), command.outputPrice(), command.status());
        modelMapper.updateById(model);
        return model;
    }

    public List<ProjectModel> projectModels(long projectId) {
        return projectModelMapper.selectList(Wrappers.<ProjectModel>lambdaQuery()
            .eq(ProjectModel::getProjectId, projectId)
            .orderByAsc(ProjectModel::getAlias));
    }

    /**
     * 校验并返回项目当前可用的模型授权，不暴露凭证内容。
     */
    public ProjectModel authorizedProjectModel(long projectId, long modelId) {
        ProjectModel projectModel = requireProjectModel(projectId, modelId);
        requireActive(projectModel.getStatus(), "项目未启用该模型");
        AiModel model = model(modelId);
        requireActive(model.getStatus(), "模型已停用");
        ModelProvider provider = provider(model.getProviderId());
        requireActive(provider.getStatus(), "模型供应商已停用");
        return projectModel;
    }

    @Transactional
    public ProjectModel grantProjectModel(long projectId, ModelCommand.GrantProjectModel command) {
        AiModel model = model(command.modelId());
        requireActive(model.getStatus(), "停用模型不能授权给项目");
        ModelProvider provider = provider(model.getProviderId());
        requireActive(provider.getStatus(), "停用供应商的模型不能授权给项目");
        String alias = normalizeAlias(command.alias());
        requireUniqueProjectAlias(projectId, alias, command.modelId());
        ProjectModel projectModel = projectModelMapper.selectOne(Wrappers.<ProjectModel>lambdaQuery()
            .eq(ProjectModel::getProjectId, projectId)
            .eq(ProjectModel::getModelId, command.modelId()));
        if (projectModel == null) {
            projectModel = new ProjectModel();
            projectModel.setProjectId(projectId);
            projectModel.setModelId(command.modelId());
        }
        projectModel.setAlias(alias);
        projectModel.setStatus(ModelStatus.ACTIVE);
        if (projectModel.getId() == null) {
            projectModelMapper.insert(projectModel);
        } else {
            projectModelMapper.updateById(projectModel);
        }
        return projectModel;
    }

    @Transactional
    public void revokeProjectModel(long projectId, long modelId) {
        ProjectModel projectModel = requireProjectModel(projectId, modelId);
        projectModel.setStatus(ModelStatus.DISABLED);
        projectModelMapper.updateById(projectModel);
    }

    /**
     * 解析项目可用模型的运行时配置，密钥仅在调用链内部短暂解密。
     */
    public ModelRuntimeConfig runtimeConfig(long projectId, long modelId) {
        ProjectModel projectModel = authorizedProjectModel(projectId, modelId);
        AiModel model = model(modelId);
        ModelProvider provider = provider(model.getProviderId());
        ModelCredential credential = credentialMapper.selectList(Wrappers.<ModelCredential>lambdaQuery()
                .eq(ModelCredential::getProviderId, provider.getId())
                .eq(ModelCredential::getStatus, ModelStatus.ACTIVE)
                .orderByDesc(ModelCredential::getId))
            .stream().findFirst()
            .orElseThrow(() -> new AiPlatformException(AiErrorCode.CONFLICT, "模型供应商未配置可用凭证"));
        String apiKey = secretCipher.decrypt(credential.getEncryptedSecret(), credential.getKeyVersion());
        return new ModelRuntimeConfig(provider.getId(), model.getId(), URI.create(provider.getBaseUrl()), apiKey,
            model.getCode());
    }

    private void applyModelSettings(AiModel model, Object capabilities, Integer contextWindow,
                                    java.math.BigDecimal inputPrice, java.math.BigDecimal outputPrice,
                                    ModelStatus status) {
        model.setCapabilities(jsonMapper.writeValueAsString(capabilities));
        model.setContextWindow(contextWindow);
        model.setInputPrice(inputPrice);
        model.setOutputPrice(outputPrice);
        model.setStatus(status);
    }

    private void requireUniqueProviderName(String name, Long excludedId) {
        List<ModelProvider> matches = providerMapper.selectList(Wrappers.<ModelProvider>lambdaQuery()
            .eq(ModelProvider::getName, name.trim()));
        if (matches.stream().anyMatch(provider -> excludedId == null || !provider.getId().equals(excludedId))) {
            throw new AiPlatformException(AiErrorCode.CONFLICT, "供应商名称已存在");
        }
    }

    private void requireUniqueModelCode(long providerId, String code, Long excludedId) {
        List<AiModel> matches = modelMapper.selectList(Wrappers.<AiModel>lambdaQuery()
            .eq(AiModel::getProviderId, providerId).eq(AiModel::getCode, code.trim()));
        if (matches.stream().anyMatch(model -> excludedId == null || !model.getId().equals(excludedId))) {
            throw new AiPlatformException(AiErrorCode.CONFLICT, "供应商下模型编码已存在");
        }
    }

    private void requireUniqueProjectAlias(long projectId, String alias, long modelId) {
        if (alias == null) {
            return;
        }
        List<ProjectModel> matches = projectModelMapper.selectList(Wrappers.<ProjectModel>lambdaQuery()
            .eq(ProjectModel::getProjectId, projectId).eq(ProjectModel::getAlias, alias));
        if (matches.stream().anyMatch(item -> !item.getModelId().equals(modelId))) {
            throw new AiPlatformException(AiErrorCode.CONFLICT, "项目内模型别名已存在");
        }
    }

    private ModelCredential requireCredential(long credentialId) {
        ModelCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            throw notFound();
        }
        return credential;
    }

    private ProjectModel requireProjectModel(long projectId, long modelId) {
        ProjectModel projectModel = projectModelMapper.selectOne(Wrappers.<ProjectModel>lambdaQuery()
            .eq(ProjectModel::getProjectId, projectId).eq(ProjectModel::getModelId, modelId));
        if (projectModel == null) {
            throw notFound();
        }
        return projectModel;
    }

    private ModelCredentialView credentialView(ModelCredential credential) {
        return new ModelCredentialView(credential.getId(), credential.getProviderId(), credential.getName(),
            credential.getSecretPrefix(), credential.getStatus());
    }

    private String normalizeBaseUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!uri.isAbsolute() || uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return uri.toString().replaceAll("/+$", "");
        } catch (IllegalArgumentException exception) {
            throw new AiPlatformException(AiErrorCode.INVALID_REQUEST, "供应商 Base URL 无效");
        }
    }

    private String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? null : alias.trim();
    }

    private String secretPrefix(String secret) {
        String normalized = secret.trim();
        return normalized.substring(0, Math.min(SECRET_PREFIX_LENGTH, normalized.length()));
    }

    private void requireActive(ModelStatus status, String message) {
        if (status != ModelStatus.ACTIVE) {
            throw new AiPlatformException(AiErrorCode.CONFLICT, message);
        }
    }

    private AiPlatformException notFound() {
        return new AiPlatformException(AiErrorCode.RESOURCE_NOT_FOUND);
    }
}
