import {expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {prepareAiHub} from '../../utils/aiHubUtils';
import {DEFAULT_MODEL} from '../../utils/copilotUtils';

export const test = mergeTests(loginTest());

test.describe('AI Hub', () => {
    test.beforeEach(async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        await new AiHubPage(page).goto();
    });

    test('loads the AI Hub page with its chats sidebar', async ({authenticatedPage: page}) => {
        await expect(page.locator('header').getByText('AI Hub', {exact: true})).toBeVisible();

        await expect(page.getByRole('link', {name: 'New Chat'})).toBeVisible();

        await expect(page.getByRole('heading', {exact: true, name: 'Chats'})).toBeVisible();
    });

    test('the chats sidebar toggles between active and archived chats', async ({authenticatedPage: page}) => {
        await page.getByRole('button', {exact: true, name: 'Archive'}).click();

        await expect(page.getByRole('heading', {exact: true, name: 'Archived chats'})).toBeVisible();

        await page.getByRole('button', {name: 'Back to active chats'}).click();

        await expect(page.getByRole('heading', {exact: true, name: 'Chats'})).toBeVisible();
    });

    test('the chats sidebar renders the search input', async ({authenticatedPage: page}) => {
        await expect(new AiHubPage(page).chatSearchInput).toBeVisible();
    });

    test('the home view renders the composer, the model picker and the suggestions', async ({
        authenticatedPage: page,
    }) => {
        const aiHubPage = new AiHubPage(page);

        await expect(page.getByRole('heading', {name: 'What should we get done?'})).toBeVisible();

        await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', 'Send a message...');

        await expect(aiHubPage.modelPickerButton).toHaveText(DEFAULT_MODEL.label);

        await expect(
            page.getByRole('button', {name: 'Build a workflow that posts new GitHub issues to a Slack channel'})
        ).toBeVisible();
    });

    test('the composer starts in Build mode and switches to Ask', async ({authenticatedPage: page}) => {
        const aiHubPage = new AiHubPage(page);

        await expect(aiHubPage.buildModeSwitch).toBeChecked();

        await aiHubPage.setBuildMode(false);

        await expect(page.getByRole('switch', {name: 'Build mode off (Ask)'})).toBeVisible();
    });

    test('the composer offers the resource picker', async ({authenticatedPage: page}) => {
        await expect(page.getByRole('button', {name: 'Add resources'})).toBeVisible();
    });
});
