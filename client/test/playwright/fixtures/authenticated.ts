/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures use 'use' callback, not React hooks */
import {Page, test as base} from '@playwright/test';

import {TEST_USER} from '../utils/constants';
import {login} from '../utils/login';

export type LoginFixturesType = {
    authenticatedPage: Page;
};

/**
 * Login test fixture - logs in the user and provides authenticatedPage.
 */
export function loginTest() {
    return base.extend<LoginFixturesType>({
        authenticatedPage: async ({page}, use) => {
            await login(page, TEST_USER.email, TEST_USER.password);

            await use(page);
        },
    });
}
