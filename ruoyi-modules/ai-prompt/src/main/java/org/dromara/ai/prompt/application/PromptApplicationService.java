package org.dromara.ai.prompt.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.common.error.AiErrorCode;
import org.dromara.ai.common.error.AiPlatformException;
import org.dromara.ai.common.project.ProjectAccessService;
import org.dromara.ai.common.project.ProjectAction;
import org.dromara.ai.model.application.ModelApplicationService;
import org.dromara.ai.prompt.domain.*;
import org.dromara.ai.prompt.infrastructure.PromptMapper;
import org.dromara.ai.prompt.infrastructure.PromptVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptApplicationService {
    private final PromptMapper promptMapper;
    private final PromptVersionMapper versionMapper;
    private final ProjectAccessService accessService;
    private final ModelApplicationService modelService;
    private final JsonMapper jsonMapper;

    @Transactional
    public Prompt create(long projectId, PromptCommand.CreatePrompt command, long userId) {
        accessService.requireAccess(userId, projectId, ProjectAction.EDIT);
        if (promptMapper.selectCount(Wrappers.<Prompt>lambdaQuery().eq(Prompt::getProjectId, projectId)
            .eq(Prompt::getCode, command.code().trim())) > 0) {
            throw new AiPlatformException(AiErrorCode.CONFLICT, "项目内 Prompt 编码已存在");
        }
        Prompt prompt = new Prompt(); prompt.setProjectId(projectId); prompt.setCode(command.code().trim());
        prompt.setName(command.name().trim()); prompt.setDescription(command.description()); prompt.setStatus(PromptStatus.ACTIVE);
        promptMapper.insert(prompt); return prompt;
    }

    public Prompt get(long projectId, long promptId, long userId) {
        accessService.requireAccess(userId, projectId, ProjectAction.VIEW);
        Prompt prompt = promptMapper.selectOne(Wrappers.<Prompt>lambdaQuery().eq(Prompt::getId, promptId).eq(Prompt::getProjectId, projectId));
        if (prompt == null) throw new AiPlatformException(AiErrorCode.RESOURCE_NOT_FOUND);
        return prompt;
    }

    public List<Prompt> list(long projectId, long userId) {
        accessService.requireAccess(userId, projectId, ProjectAction.VIEW);
        return promptMapper.selectList(Wrappers.<Prompt>lambdaQuery().eq(Prompt::getProjectId, projectId).orderByDesc(Prompt::getId));
    }

    @Transactional
    public PromptVersion createDraft(long projectId, long promptId, PromptCommand.Draft command, long userId) {
        Prompt prompt = get(projectId, promptId, userId);
        accessService.requireAccess(userId, projectId, ProjectAction.EDIT);
        modelService.authorizedProjectModel(projectId, command.modelId());
        List<PromptVariable> variables = command.variables() == null ? List.of() : command.variables();
        String variablesJson = jsonMapper.writeValueAsString(variables);
        PromptSchema input = PromptSchema.parse(command.inputSchema(), jsonMapper);
        PromptSchema output = PromptSchema.parse(command.outputSchema(), jsonMapper);
        validateTemplate(command.systemTemplate(), command.userTemplate(), variables);
        int next = versionMapper.selectList(Wrappers.<PromptVersion>lambdaQuery().eq(PromptVersion::getPromptId, promptId))
            .stream().mapToInt(v -> v.getVersionNo() == null ? 0 : v.getVersionNo()).max().orElse(0) + 1;
        PromptVersion version = new PromptVersion(); version.setProjectId(projectId); version.setPromptId(promptId); version.setVersionNo(next);
        version.setSystemTemplate(command.systemTemplate()); version.setUserTemplate(command.userTemplate()); version.setVariables(variablesJson);
        version.setInputSchema(command.inputSchema() == null || command.inputSchema().isBlank() ? null : input.toJson());
        version.setOutputSchema(command.outputSchema() == null || command.outputSchema().isBlank() ? null : output.toJson());
        version.setModelId(command.modelId()); version.setModelParameters(jsonMapper.writeValueAsString(command.modelParameters() == null ? Map.of() : command.modelParameters()));
        version.setStatus(PromptVersionStatus.DRAFT); version.setSuccessfulTest(false); version.setLockVersion(0); version.setChangeNote(command.changeNote());
        versionMapper.insert(version); prompt.setCurrentDraftVersionId(version.getId()); promptMapper.updateById(prompt); return version;
    }

    public PromptVersion version(long projectId, long versionId, long userId) {
        accessService.requireAccess(userId, projectId, ProjectAction.VIEW);
        PromptVersion v = versionMapper.selectOne(Wrappers.<PromptVersion>lambdaQuery().eq(PromptVersion::getId, versionId).eq(PromptVersion::getProjectId, projectId));
        if (v == null) throw new AiPlatformException(AiErrorCode.RESOURCE_NOT_FOUND); return v;
    }

    public void validateTemplate(String systemTemplate, String userTemplate, List<PromptVariable> variables) {
        List<PromptVariable> definitions = variables == null ? List.of() : variables;
        java.util.Set<String> names = definitions.stream().map(PromptVariable::name).collect(java.util.stream.Collectors.toSet());
        if (names.size() != definitions.size()) {
            throw new AiPlatformException(AiErrorCode.INVALID_REQUEST, "变量名不能重复");
        }
        String text = (systemTemplate == null ? "" : systemTemplate) + "\n" + (userTemplate == null ? "" : userTemplate);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}") .matcher(text);
        while (matcher.find()) if (!names.contains(matcher.group(1))) throw new AiPlatformException(AiErrorCode.INVALID_REQUEST, "模板引用了未声明变量: " + matcher.group(1));
        if (userTemplate == null || userTemplate.isBlank()) throw new AiPlatformException(AiErrorCode.INVALID_REQUEST, "用户模板不能为空");
    }
}
