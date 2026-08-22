import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { ProjectForm, ProjectMemberVO, ProjectRole, ProjectVO } from './types';

export const listProjects = (): AxiosPromise<ProjectVO[]> => request({ url: '/ai/projects', method: 'get' });
export const getProject = (id: string | number): AxiosPromise<ProjectVO> => request({ url: `/ai/projects/${id}`, method: 'get' });
export const addProject = (data: ProjectForm): AxiosPromise<ProjectVO> => request({ url: '/ai/projects', method: 'post', data });
export const updateProject = (id: string | number, data: Omit<ProjectForm, 'code'>): AxiosPromise<ProjectVO> =>
  request({ url: `/ai/projects/${id}`, method: 'put', data });
export const archiveProject = (id: string | number) => request({ url: `/ai/projects/${id}/archive`, method: 'post' });
export const listMembers = (projectId: string | number): AxiosPromise<ProjectMemberVO[]> =>
  request({ url: `/ai/projects/${projectId}/members`, method: 'get' });
export const addMember = (projectId: string | number, userId: string | number, role: ProjectRole) =>
  request({ url: `/ai/projects/${projectId}/members`, method: 'post', data: { userId, role } });
export const changeMemberRole = (projectId: string | number, userId: string | number, role: ProjectRole) =>
  request({ url: `/ai/projects/${projectId}/members/${userId}`, method: 'put', data: { role } });
export const removeMember = (projectId: string | number, userId: string | number) =>
  request({ url: `/ai/projects/${projectId}/members/${userId}`, method: 'delete' });
export const transferOwner = (projectId: string | number, userId: string | number) =>
  request({ url: `/ai/projects/${projectId}/members/transfer-owner`, method: 'post', data: { userId } });
