import {Page} from '@playwright/test';

import {TEST_USER} from './constants';
import {login} from './login';

/**
 * Ensures user is logged in. If already authenticated, does nothing.
 * Useful for utilities that need authentication but don't want to fail if already logged in.
 */
export async function ensureAuthenticated(page: Page): Promise<void> {
    await page.goto('/');
    await page.waitForTimeout(1000);

    if (!page.url().includes('/login')) {
        return;
    }

    await login(page, TEST_USER.email, TEST_USER.password);
}
