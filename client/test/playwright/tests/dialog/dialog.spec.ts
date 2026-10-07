import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {ProjectsPage} from '../../pages/projectsPage';
import {ROUTES} from '../../utils/constants';

export const test = mergeTests(loginTest());

const GLOBAL_SEARCH_FEATURE_FLAG = 'ff-2396';

const DIALOG_WIDTH = 512;

/**
 * The global search palette sits behind a feature flag the server reports through /actuator/info, so the response
 * is patched to switch it on before the app reads it.
 */
async function enableGlobalSearch(page: Page): Promise<void> {
    await page.route('**/actuator/info', async (route) => {
        const response = await route.fetch();
        const applicationInfo = await response.json();

        await route.fulfill({
            json: {
                ...applicationInfo,
                featureFlags: {...applicationInfo.featureFlags, [GLOBAL_SEARCH_FEATURE_FLAG]: true},
            },
            response,
        });
    });
}

async function openProjectsPage(page: Page): Promise<void> {
    await page.goto(ROUTES.projects);

    await new ProjectsPage(page).waitForPageLoad();
}

test.describe('Global search command palette', () => {
    test.beforeEach(async ({authenticatedPage: page}) => {
        await enableGlobalSearch(page);

        await openProjectsPage(page);
    });

    async function openCommandPalette(page: Page) {
        const commandPalette = page.getByRole('dialog', {name: 'Command Palette'});

        // The shortcut listener is attached once the feature flag has loaded, so retry the keypress until it lands.
        await expect(async () => {
            await page.keyboard.press('ControlOrMeta+k');

            await expect(commandPalette).toBeVisible({timeout: 1000});
        }).toPass();

        return commandPalette;
    }

    test('should open from the keyboard shortcut with the search input focused and a close button', async ({
        authenticatedPage: page,
    }) => {
        const commandPalette = await openCommandPalette(page);

        await expect(commandPalette.getByPlaceholder('Search projects, workflows, connections...')).toBeFocused();
        await expect(commandPalette.getByRole('button', {name: 'Close'})).toBeVisible();
        await expect(commandPalette.getByText('Type at least 2 characters to search...')).toBeVisible();
    });

    test('should close from its close button', async ({authenticatedPage: page}) => {
        const commandPalette = await openCommandPalette(page);

        await commandPalette.getByRole('button', {name: 'Close'}).click();

        await expect(commandPalette).toBeHidden();
    });

    test('should close on Escape', async ({authenticatedPage: page}) => {
        const commandPalette = await openCommandPalette(page);

        await page.keyboard.press('Escape');

        await expect(commandPalette).toBeHidden();
    });
});

test.describe('Dialog family', () => {
    test.beforeEach(async ({authenticatedPage: page}) => {
        await openProjectsPage(page);
    });

    async function openCreateProjectDialog(page: Page) {
        const projectsPage = new ProjectsPage(page);

        await projectsPage.createProjectButton.click();

        await expect(projectsPage.createProjectDialog).toBeVisible();

        return projectsPage.createProjectDialog;
    }

    test('should center the dialog over a dimmed overlay at the sm width', async ({authenticatedPage: page}) => {
        const createProjectDialog = await openCreateProjectDialog(page);

        await expect(page.locator('[data-slot="dialog-overlay"]')).toBeVisible();
        await expect(createProjectDialog).toHaveAttribute('data-slot', 'dialog-content');

        const viewportSize = page.viewportSize()!;

        // The open animation zooms in from 95%, so measure once it has settled.
        await expect(async () => {
            const dialogBox = (await createProjectDialog.boundingBox())!;

            expect(dialogBox.width).toBeCloseTo(DIALOG_WIDTH, 0);
            expect(dialogBox.x + dialogBox.width / 2).toBeCloseTo(viewportSize.width / 2, 0);
            expect(dialogBox.y + dialogBox.height / 2).toBeCloseTo(viewportSize.height / 2, 0);
        }).toPass();
    });

    test('should paint its card through DialogMain and title it through DialogHeader', async ({
        authenticatedPage: page,
    }) => {
        const createProjectDialog = await openCreateProjectDialog(page);

        await expect(createProjectDialog.locator('[data-slot="dialog-main"]')).toBeVisible();
        await expect(createProjectDialog.getByRole('heading', {name: 'Create Project'})).toBeVisible();
        await expect(createProjectDialog.getByRole('button', {name: 'Close'})).toBeVisible();
    });

    test('should close on Escape', async ({authenticatedPage: page}) => {
        const createProjectDialog = await openCreateProjectDialog(page);

        await page.keyboard.press('Escape');

        await expect(createProjectDialog).toBeHidden();
        await expect(page.locator('[data-slot="dialog-overlay"]')).toHaveCount(0);
    });

    test('should close from its Cancel button', async ({authenticatedPage: page}) => {
        const createProjectDialog = await openCreateProjectDialog(page);

        await createProjectDialog.getByRole('button', {name: 'Cancel'}).click();

        await expect(createProjectDialog).toBeHidden();
    });
});
