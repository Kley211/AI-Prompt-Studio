package org.dromara.ai.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_project_model")
public class ProjectModel extends BaseEntity {
    @TableId private Long id;
    private Long projectId;
    private Long modelId;
    private String alias;
    private ModelStatus status;
    @TableLogic private Long delFlag;
}
