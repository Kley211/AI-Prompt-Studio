package org.dromara.ai.project.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.project.domain.Project;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> { }
