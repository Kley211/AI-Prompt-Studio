<template>
  <div class="p-2 app-container">
    <el-card shadow="hover">
      <template #header>
        <div class="toolbar-shell">
          <div class="table-heading">
            <h3>项目工作空间</h3>
            <span>AI 资源必须归属项目，成员角色决定查看、编辑、发布和成员管理权限。</span>
          </div>
          <el-button v-hasPermi="['ai:project:add']" type="primary" icon="Plus" @click="openCreate">新建项目</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="projects" border>
        <el-table-column label="项目名称" prop="name" min-width="160" />
        <el-table-column label="编码" prop="code" min-width="140" />
        <el-table-column label="数据留存" width="150">
          <template #default="{ row }">{{ retentionLabels[row.retentionMode] }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? '启用' : '已归档' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责人 ID" prop="ownerId" min-width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['ai:project:edit']" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-hasPermi="['ai:project:member']" link type="primary" @click="openMembers(row)">成员</el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              v-hasPermi="['ai:project:edit']"
              link
              type="danger"
              @click="handleArchive(row)"
            >归档</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && projects.length === 0" description="暂无可访问项目" />
    </el-card>

    <el-dialog v-model="projectDialog.visible" :title="projectDialog.title" width="560px" append-to-body>
      <el-form ref="projectFormRef" :model="projectForm" :rules="projectRules" label-width="95px">
        <el-form-item label="项目编码" prop="code">
          <el-input v-model="projectForm.code" :disabled="Boolean(editingId)" placeholder="例如 marketing-ai" />
        </el-form-item>
        <el-form-item label="项目名称" prop="name"><el-input v-model="projectForm.name" /></el-form-item>
        <el-form-item label="数据留存" prop="retentionMode">
          <el-select v-model="projectForm.retentionMode" class="w-full">
            <el-option v-for="(label, value) in retentionLabels" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目说明" prop="description">
          <el-input v-model="projectForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="projectDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProject">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="memberDrawer" :title="`${activeProject?.name || ''} · 成员管理`" size="680px">
      <el-form :inline="true" :model="memberForm">
        <el-form-item label="用户 ID"><el-input v-model="memberForm.userId" placeholder="RuoYi 用户 ID" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="memberForm.role" style="width: 150px"><el-option v-for="role in roles" :key="role" :label="roleLabels[role]" :value="role" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="handleAddMember">添加成员</el-button></el-form-item>
      </el-form>
      <el-alert title="OWNER 不可直接移除；请先将所有权转移给其他有效成员。" type="info" :closable="false" class="mb-4" />
      <el-table v-loading="memberLoading" :data="members" border>
        <el-table-column label="用户 ID" prop="userId" min-width="180" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.role" :disabled="row.role === 'OWNER'" @change="handleRoleChange(row)">
              <el-option v-for="role in roles.filter(item => item !== 'OWNER')" :key="role" :label="roleLabels[role]" :value="role" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.role !== 'OWNER'" link type="primary" @click="handleTransfer(row)">转为负责人</el-button>
            <el-button v-if="row.role !== 'OWNER'" link type="danger" @click="handleRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup name="AiProject" lang="ts">
import { addMember, addProject, archiveProject, changeMemberRole, listMembers, listProjects, removeMember, transferOwner, updateProject } from '@/api/ai/project';
import type { ProjectForm, ProjectMemberVO, ProjectRole, ProjectVO, RetentionMode } from '@/api/ai/project/types';
import modal from '@/plugins/modal';

const projects = ref<ProjectVO[]>([]);
const members = ref<ProjectMemberVO[]>([]);
const loading = ref(false);
const memberLoading = ref(false);
const submitting = ref(false);
const editingId = ref<string | number>();
const memberDrawer = ref(false);
const activeProject = ref<ProjectVO>();
const projectFormRef = ref<ElFormInstance>();
const projectDialog = reactive({ visible: false, title: '' });
const retentionLabels: Record<RetentionMode, string> = { FULL: '完整内容', MASKED: '脱敏内容', METADATA_ONLY: '仅元数据' };
const roles: ProjectRole[] = ['OWNER', 'ADMIN', 'DEVELOPER', 'PUBLISHER', 'VIEWER'];
const roleLabels: Record<ProjectRole, string> = { OWNER: '负责人', ADMIN: '管理员', DEVELOPER: '开发者', PUBLISHER: '发布者', VIEWER: '观察者' };
const projectForm = reactive<ProjectForm>({ code: '', name: '', description: '', retentionMode: 'MASKED' });
const memberForm = reactive<{ userId: string; role: ProjectRole }>({ userId: '', role: 'VIEWER' });
const projectRules = {
  code: [{ required: true, message: '请输入项目编码', trigger: 'blur' }, { pattern: /^[a-z][a-z0-9-]{2,63}$/, message: '以小写字母开头，只能包含小写字母、数字和短横线', trigger: 'blur' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  retentionMode: [{ required: true, message: '请选择数据留存策略', trigger: 'change' }]
};

const loadProjects = async () => { loading.value = true; try { projects.value = (await listProjects()).data || []; } finally { loading.value = false; } };
const resetProjectForm = () => Object.assign(projectForm, { code: '', name: '', description: '', retentionMode: 'MASKED' });
const openCreate = () => { editingId.value = undefined; resetProjectForm(); projectDialog.title = '新建项目'; projectDialog.visible = true; };
const openEdit = (row: ProjectVO) => { editingId.value = row.id; Object.assign(projectForm, row); projectDialog.title = '编辑项目'; projectDialog.visible = true; };
const submitProject = async () => {
  await projectFormRef.value?.validate(); submitting.value = true;
  try {
    if (editingId.value) await updateProject(editingId.value, { name: projectForm.name, description: projectForm.description, retentionMode: projectForm.retentionMode });
    else await addProject(projectForm);
    modal.msgSuccess('保存成功'); projectDialog.visible = false; await loadProjects();
  } finally { submitting.value = false; }
};
const handleArchive = async (row: ProjectVO) => { await modal.confirm(`确认归档项目“${row.name}”吗？`); await archiveProject(row.id); modal.msgSuccess('已归档'); await loadProjects(); };
const loadMembers = async () => { if (!activeProject.value) return; memberLoading.value = true; try { members.value = (await listMembers(activeProject.value.id)).data || []; } finally { memberLoading.value = false; } };
const openMembers = async (row: ProjectVO) => { activeProject.value = row; memberDrawer.value = true; await loadMembers(); };
const handleAddMember = async () => { if (!activeProject.value || !memberForm.userId) return modal.msgWarning('请输入用户 ID'); await addMember(activeProject.value.id, memberForm.userId, memberForm.role); memberForm.userId = ''; modal.msgSuccess('成员已添加'); await loadMembers(); };
const handleRoleChange = async (row: ProjectMemberVO) => { if (!activeProject.value) return; await changeMemberRole(activeProject.value.id, row.userId, row.role); modal.msgSuccess('角色已更新'); };
const handleRemove = async (row: ProjectMemberVO) => { if (!activeProject.value) return; await modal.confirm(`确认移除用户 ${row.userId} 吗？`); await removeMember(activeProject.value.id, row.userId); await loadMembers(); };
const handleTransfer = async (row: ProjectMemberVO) => { if (!activeProject.value) return; await modal.confirm(`确认将项目所有权转移给用户 ${row.userId} 吗？`); await transferOwner(activeProject.value.id, row.userId); modal.msgSuccess('所有权已转移'); await Promise.all([loadMembers(), loadProjects()]); };

onMounted(loadProjects);
</script>
