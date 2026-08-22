package org.dromara.ai.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_credential")
public class ModelCredential extends BaseEntity {
    @TableId private Long id;
    private Long providerId;
    private String name;
    private String secretPrefix;
    private String encryptedSecret;
    private String keyVersion;
    private ModelStatus status;
    @TableLogic private Long delFlag;
}
