package org.dromara.ai.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_project")
public class Project extends BaseEntity {
    @TableId private Long id;
    private String code;
    private String name;
    private String description;
    private ProjectStatus status;
    private RetentionMode retentionMode;
    private Long ownerId;
    @TableLogic private Long delFlag;
}
