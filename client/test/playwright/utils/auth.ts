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

/**
 * Waits out a long stretch (an approval expiry or a sweep period) and then starts a fresh session, so the API calls
 * after the wait carry a valid session and XSRF token.
 */
export async function waitAndStaySignedIn(page: Page, milliseconds: number): Promise<void> {
    await page.waitForTimeout(Math.max(milliseconds, 0));

    await page.context().clearCookies();

    await login(page, TEST_USER.email, TEST_USER.password);
}
