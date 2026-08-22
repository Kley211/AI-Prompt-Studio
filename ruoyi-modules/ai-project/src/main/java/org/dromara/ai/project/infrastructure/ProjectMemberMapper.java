package org.dromara.ai.project.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.ai.project.domain.ProjectMember;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {
    ProjectMember selectActiveMember(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
