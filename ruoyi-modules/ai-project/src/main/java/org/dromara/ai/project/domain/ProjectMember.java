package org.dromara.ai.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_project_member")
public class ProjectMember extends BaseEntity {
    @TableId private Long id;
    private Long projectId;
    private Long userId;
    private ProjectRole role;
    private String status;
    @TableLogic private Long delFlag;
}
