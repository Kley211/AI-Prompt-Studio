import { expect, test } from '@playwright/test';

test.describe('US1 项目工作空间与权限', () => {
  test('创建项目、管理成员并展示角色限制', async ({ page }) => {
    await page.goto('/login');
    await page.getByPlaceholder('账号').fill(process.env.E2E_USERNAME ?? 'admin');
    await page.getByPlaceholder('密码').fill(process.env.E2E_PASSWORD ?? 'admin123');
    await page.getByRole('button', { name: /登录/ }).click();
    await expect(page).toHaveURL(/dashboard|index/);

    await page.goto('/ai/project');
    await page.getByRole('button', { name: '新建项目' }).click();
    await page.getByLabel('项目编码').fill(`e2e-project-${Date.now()}`);
    await page.getByLabel('项目名称').fill('US1 验收项目');
    await page.getByRole('button', { name: '保存' }).click();
    await expect(page.getByText('US1 验收项目')).toBeVisible();

    await page.getByRole('button', { name: '成员' }).first().click();
    await expect(page.getByText(/成员管理/)).toBeVisible();
    await expect(page.getByText(/OWNER 不可直接移除/)).toBeVisible();

    await page.getByPlaceholder('RuoYi 用户 ID').fill(process.env.E2E_MEMBER_USER_ID ?? '2');
    await page.getByRole('button', { name: '添加成员' }).click();
    await expect(page.getByText('成员已添加')).toBeVisible();
    await expect(page.getByRole('cell', { name: process.env.E2E_MEMBER_USER_ID ?? '2', exact: true })).toBeVisible();
  });

  test('VIEWER 无法执行项目编辑操作', async ({ page }) => {
    test.skip(!process.env.E2E_VIEWER_USERNAME || !process.env.E2E_VIEWER_PASSWORD, '需要配置 VIEWER 验收账号');

    await page.goto('/login');
    await page.getByPlaceholder('账号').fill(process.env.E2E_VIEWER_USERNAME!);
    await page.getByPlaceholder('密码').fill(process.env.E2E_VIEWER_PASSWORD!);
    await page.getByRole('button', { name: /登录/ }).click();
    await page.goto('/ai/project');

    await expect(page.getByRole('button', { name: '编辑' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '归档' })).toHaveCount(0);
  });
});
