import {expect, test} from '@playwright/test';

import {ensureAuthenticated} from '../../utils/auth';
import getRandomString from '../../utils/getRandomString';

test.describe('Workspace Files', () => {
    test.beforeEach(async ({page}) => {
        await ensureAuthenticated(page);

        await page.goto('/automation/workspace-files');

        await page.waitForLoadState('domcontentloaded');

        await expect(page).toHaveURL(/\/automation\/workspace-files/);
    });

    test('should upload a markdown file, edit its content, and persist the change after reload', async ({page}) => {
        const randomString = getRandomString();
        const fileName = `e2e_${randomString}.md`;
        const initialContent = '# e2e';
        const editedContent = '# e2e edited';

        let uploadedFileTestId = '';

        await test.step('Upload a markdown file', async () => {
            const fileInput = page.getByTestId('workspace-file-input');

            await fileInput.setInputFiles({
                buffer: Buffer.from(initialContent),
                mimeType: 'text/markdown',
                name: fileName,
            });

            const uploadedRow = page
                .locator('[data-testid^="workspace-file-row-"]')
                .filter({hasText: fileName})
                .first();

            await expect(uploadedRow).toBeVisible({timeout: 15000});

            const testIdValue = await uploadedRow.getAttribute('data-testid');

            expect(testIdValue).not.toBeNull();

            uploadedFileTestId = testIdValue as string;
        });

        await test.step('Open the file in the detail sheet and edit its content', async () => {
            await page.getByTestId(uploadedFileTestId).click();

            const sheet = page.getByTestId('workspace-file-detail-sheet');

            await expect(sheet).toBeVisible({timeout: 10000});

            const monacoContainer = page.getByTestId('workspace-file-monaco');

            await expect(monacoContainer).toBeVisible({timeout: 15000});

            const editor = monacoContainer.locator('.monaco-editor textarea').first();

            await expect(editor).toBeAttached({timeout: 15000});

            await editor.click();

            await page.keyboard.press('ControlOrMeta+A');

            await page.keyboard.press('Delete');

            await page.keyboard.type(editedContent);

            await sheet.getByRole('button', {name: 'Save'}).click();

            await expect(page.getByText('File saved')).toBeVisible({timeout: 10000});
        });

        await test.step('Reload the page and assert the edited content is persisted', async () => {
            await page.reload();

            await page.waitForLoadState('domcontentloaded');

            const persistedRow = page.getByTestId(uploadedFileTestId);

            await expect(persistedRow).toBeVisible({timeout: 15000});

            await persistedRow.click();

            const monacoContainer = page.getByTestId('workspace-file-monaco');

            await expect(monacoContainer).toBeVisible({timeout: 15000});

            await expect(monacoContainer).toContainText(editedContent, {timeout: 15000});
        });
    });
});
