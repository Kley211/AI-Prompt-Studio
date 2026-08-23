package org.dromara.ai.prompt.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_prompt_version")
public class PromptVersion extends BaseEntity {
    @TableId private Long id;
    private Long projectId;
    private Long promptId;
    private Integer versionNo;
    private String systemTemplate;
    private String userTemplate;
    private String variables;
    private String inputSchema;
    private String outputSchema;
    private Long modelId;
    private String modelParameters;
    private PromptVersionStatus status;
    private String changeNote;
    private Boolean successfulTest;
    private Integer lockVersion;
    @TableLogic private Long delFlag;
}
