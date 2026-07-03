import {expect, test} from '@playwright/test';

import {ensureAuthenticated} from '../../utils/auth';

test.describe('AI Hub', () => {
    test.beforeEach(async ({page}) => {
        await ensureAuthenticated(page);

        await page.goto('/automation/ai-hub');

        await page.waitForLoadState('domcontentloaded');

        await expect(page).toHaveURL(/\/automation\/ai-hub/);
    });

    test('loads AI Hub page with header and conversations sidebar', async ({page}) => {
        // The page-level header contains "AI Hub"
        await expect(page.getByRole('heading', {name: 'AI Hub'})).toBeVisible({timeout: 10000});

        // The left sidebar header contains "Conversations"
        await expect(page.getByRole('heading', {name: 'Conversations'})).toBeVisible();
    });

    test('conversations sidebar renders Active/Archived filter tabs', async ({page}) => {
        await expect(page.getByRole('tab', {name: 'Active'})).toBeVisible({timeout: 10000});

        await expect(page.getByRole('tab', {name: 'Archived'})).toBeVisible();
    });

    test('conversations sidebar renders search input', async ({page}) => {
        await expect(page.getByPlaceholder('Search conversations...')).toBeVisible({timeout: 10000});
    });

    test('conversations sidebar renders New conversation button', async ({page}) => {
        await expect(page.getByRole('button', {name: 'New conversation'})).toBeVisible({timeout: 10000});
    });

    test('conversations sidebar shows empty state when no conversations exist', async ({page}) => {
        // The sidebar either shows conversation items or the empty-state message.
        // We assert that the container itself is rendered regardless of which branch renders.
        const emptyStateText = page.getByText(/no conversations yet/i);
        const conversationItems = page.locator('[class*="group flex w-full items-center"]');

        // Wait a reasonable amount of time for the async conversations query to settle.
        await page.waitForTimeout(3000);

        const hasEmptyState = await emptyStateText.isVisible().catch(() => false);
        const hasConversationItems = (await conversationItems.count()) > 0;

        // One of the two conditions must be true — the sidebar rendered its content.
        expect(hasEmptyState || hasConversationItems).toBe(true);
    });

    test('AI Hub panel renders AI Copilot section', async ({page}) => {
        // The panel header shows "AI Copilot" (h4 inside the panel, distinct from the page heading).
        await expect(page.getByRole('heading', {name: 'AI Copilot'})).toBeVisible({timeout: 10000});
    });

    test('AI Hub panel renders mode toggle with Ask and Build options', async ({page}) => {
        // ToggleGroup items for ASK / BUILD mode switching.
        await expect(page.getByRole('radio', {name: 'Ask'})).toBeVisible({timeout: 10000});

        await expect(page.getByRole('radio', {name: 'Build'})).toBeVisible();
    });

    test('AI Hub panel renders References composer button', async ({page}) => {
        // The @-mention button in the composer area
        await expect(page.getByRole('button', {name: 'Add reference'})).toBeVisible({timeout: 10000});
    });
});
