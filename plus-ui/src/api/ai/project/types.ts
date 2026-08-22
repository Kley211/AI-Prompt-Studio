export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';
export type RetentionMode = 'FULL' | 'MASKED' | 'METADATA_ONLY';
export type ProjectRole = 'OWNER' | 'ADMIN' | 'DEVELOPER' | 'PUBLISHER' | 'VIEWER';

export interface ProjectVO extends BaseEntity {
  id: string | number;
  code: string;
  name: string;
  description?: string;
  status: ProjectStatus;
  retentionMode: RetentionMode;
  ownerId: string | number;
}

export interface ProjectForm {
  code: string;
  name: string;
  description?: string;
  retentionMode: RetentionMode;
}

export interface ProjectMemberVO extends BaseEntity {
  id: string | number;
  projectId: string | number;
  userId: string | number;
  role: ProjectRole;
  status: 'ACTIVE' | 'REMOVED';
}
