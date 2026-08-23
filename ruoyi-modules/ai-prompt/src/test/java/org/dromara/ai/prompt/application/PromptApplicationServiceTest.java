package org.dromara.ai.prompt.application;

import org.dromara.ai.common.error.AiErrorCode;
import org.dromara.ai.common.error.AiPlatformException;
import org.dromara.ai.common.project.ProjectAccessService;
import org.dromara.ai.model.application.ModelApplicationService;
import org.dromara.ai.model.domain.ModelStatus;
import org.dromara.ai.model.domain.ProjectModel;
import org.dromara.ai.prompt.domain.Prompt;
import org.dromara.ai.prompt.domain.PromptVersion;
import org.dromara.ai.prompt.domain.PromptVersionStatus;
import org.dromara.ai.prompt.domain.PromptVariable;
import org.dromara.ai.prompt.domain.PromptVariableType;
import org.dromara.ai.prompt.infrastructure.PromptMapper;
import org.dromara.ai.prompt.infrastructure.PromptVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PromptApplicationServiceTest {
    private PromptApplicationService service;
    private PromptMapper promptMapper;
    private PromptVersionMapper versionMapper;
    private ModelApplicationService modelService;

    @BeforeEach
    void setUp() {
        promptMapper = mock(PromptMapper.class);
        versionMapper = mock(PromptVersionMapper.class);
        modelService = mock(ModelApplicationService.class);
        service = new PromptApplicationService(promptMapper, versionMapper,
            mock(ProjectAccessService.class), modelService, JsonMapper.builder().build());
    }

    @Test
    void acceptsDeclaredTemplateVariables() {
        assertDoesNotThrow(() -> service.validateTemplate("你是助手", "你好，{{ name }}",
            List.of(new PromptVariable("name", PromptVariableType.STRING, true, null, null))));
    }

    @Test
    void rejectsUndeclaredTemplateVariable() {
        AiPlatformException exception = assertThrows(AiPlatformException.class,
            () -> service.validateTemplate(null, "你好，{{name}}", List.of()));
        assertEquals(AiErrorCode.INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void rejectsDuplicateVariableDefinitions() {
        PromptVariable variable = new PromptVariable("name", PromptVariableType.STRING, true, null, null);
        AiPlatformException exception = assertThrows(AiPlatformException.class,
            () -> service.validateTemplate(null, "{{name}}", List.of(variable, variable)));
        assertEquals("变量名不能重复", exception.getMessage());
    }

    @Test
    void rejectsBlankUserTemplate() {
        AiPlatformException exception = assertThrows(AiPlatformException.class,
            () -> service.validateTemplate(null, " ", List.of()));
        assertEquals(AiErrorCode.INVALID_REQUEST, exception.errorCode());
    }

    @Test
    void createsNextDraftAndUpdatesDraftPointer() {
        Prompt prompt = new Prompt(); prompt.setId(10L); prompt.setProjectId(20L);
        when(promptMapper.selectOne(any())).thenReturn(prompt);
        PromptVersion old = new PromptVersion(); old.setVersionNo(2);
        when(versionMapper.selectList(any())).thenReturn(List.of(old));
        ProjectModel projectModel = new ProjectModel(); projectModel.setProjectId(20L); projectModel.setModelId(30L);
        projectModel.setStatus(ModelStatus.ACTIVE);
        when(modelService.authorizedProjectModel(20L, 30L)).thenReturn(projectModel);
        doAnswer(invocation -> { ((PromptVersion) invocation.getArgument(0)).setId(40L); return 1; })
            .when(versionMapper).insert(any(PromptVersion.class));

        PromptVersion created = service.createDraft(20L, 10L, new PromptCommand.Draft(
            "你好 {{name}}", "你是助手", List.of(new PromptVariable("name", PromptVariableType.STRING,
            true, null, null)), "{\"type\":\"object\"}", null, 30L, Map.of("temperature", 0.2), "第三版"), 99L);

        assertEquals(3, created.getVersionNo());
        assertEquals(PromptVersionStatus.DRAFT, created.getStatus());
        assertEquals(40L, prompt.getCurrentDraftVersionId());
        verify(promptMapper).updateById(prompt);
    }
}
