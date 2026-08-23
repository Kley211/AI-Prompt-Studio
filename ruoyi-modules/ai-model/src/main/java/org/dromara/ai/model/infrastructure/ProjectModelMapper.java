package org.dromara.ai.model.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.model.domain.ProjectModel;

@Mapper
public interface ProjectModelMapper extends BaseMapper<ProjectModel> {
}
