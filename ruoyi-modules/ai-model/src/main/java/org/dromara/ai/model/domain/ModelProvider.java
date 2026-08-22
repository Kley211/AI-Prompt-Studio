package org.dromara.ai.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_provider")
public class ModelProvider extends BaseEntity {
    @TableId private Long id;
    private String name;
    private ProviderProtocol protocol;
    private String baseUrl;
    private String description;
    private ModelStatus status;
    @TableLogic private Long delFlag;
}
