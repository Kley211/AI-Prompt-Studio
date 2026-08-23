package org.dromara.ai.prompt.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_prompt")
public class Prompt extends BaseEntity {
    @TableId private Long id;
    private Long projectId;
    private String code;
    private String name;
    private String description;
    private Long currentDraftVersionId;
    private Long currentPublishedVersionId;
    private PromptStatus status;
    @TableLogic private Long delFlag;
}
