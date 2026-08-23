package org.dromara.ai.prompt.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.prompt.domain.Prompt;

@Mapper
public interface PromptMapper extends BaseMapper<Prompt> { }
