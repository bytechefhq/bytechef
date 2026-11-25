import {expect} from '@playwright/test';

import {loginTest, mergeTests, projectTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import {getProjectWorkspaceId} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';
import {login} from '../../utils/login';
import {
    PROMOTE_TO_ENVIRONMENT_MUTATION,
    STAGING_ENVIRONMENT_ID,
    buildPlainWorkflow,
    createA2aServer,
    createDevelopmentDeployment,
    deleteA2aServerQuietly,
    deleteProjectDeploymentQuietly,
    getPromotionPreview,
    openRowMenu,
    setCurrentEnvironment,
    skipUnlessPromotionIsAvailable,
} from '../../utils/promotionUtils';

const NON_ADMIN_USER = {
    email: process.env.PLAYWRIGHT_NON_ADMIN_EMAIL || 'user@localhost.com',
    password: process.env.PLAYWRIGHT_NON_ADMIN_PASSWORD || 'user',
};

const test = mergeTests(loginTest(), projectTest);

test.describe('Promotion guards', () => {
    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test('refuses to promote an A2A server for a user without the admin role', async ({
        authenticatedPage: page,
        browser,
        project,
    }) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const a2aServerId = await createA2aServer(page, workspaceId, `server_${getRandomString()}`);

        const nonAdminContext = await browser.newContext();

        try {
            const nonAdminPage = await nonAdminContext.newPage();

            await login(nonAdminPage, NON_ADMIN_USER.email, NON_ADMIN_USER.password);

            const accountResponse = await nonAdminPage.request.get('/api/account');

            const account = accountResponse.ok() ? await accountResponse.json() : undefined;

            test.skip(
                !account || (account.authorities ?? []).includes('ROLE_ADMIN'),
                'Needs a non-admin user; set PLAYWRIGHT_NON_ADMIN_EMAIL and PLAYWRIGHT_NON_ADMIN_PASSWORD'
            );

            const xsrfToken = (await nonAdminContext.cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN')?.value;

            const promotionResponse = await nonAdminPage.request.post('/graphql', {
                data: {
                    query: PROMOTE_TO_ENVIRONMENT_MUTATION,
                    variables: {
                        input: {
                            connectionMappings: [],
                            resourceType: 'A2A_SERVER',
                            sourceId: a2aServerId,
                            targetEnvironmentId: String(STAGING_ENVIRONMENT_ID),
                        },
                    },
                },
                headers: xsrfToken ? {'X-XSRF-TOKEN': xsrfToken} : {},
            });

            const promotionBody = await promotionResponse.json().catch(() => undefined);

            expect(promotionBody?.data?.promoteToEnvironment ?? null).toBeNull();

            expect((await getPromotionPreview(page, 'A2A_SERVER', a2aServerId)).existingTargetId).toBeNull();
        } finally {
            await nonAdminContext.close();

            await deleteA2aServerQuietly(page, a2aServerId);
        }
    });

    test('hides Promote to environment when only one environment exists', async ({
        authenticatedPage: page,
        project,
    }) => {
        const developmentDeployment = await createDevelopmentDeployment({
            page,
            projectId: project.id,
            workflow: buildPlainWorkflow(`workflow_${getRandomString()}`),
        });

        try {
            await page.route('**/graphql', async (route) => {
                if (!route.request().postData()?.includes('query environments')) {
                    await route.fallback();

                    return;
                }

                await route.fulfill({json: {data: {environments: [{id: '0', name: 'DEVELOPMENT'}]}}});
            });

            await page.goto(`/automation/projects?projectId=${project.id}&tab=deployments`);

            const promoteMenuItem = await openRowMenu(page, developmentDeployment.deploymentName);

            await expect(page.getByRole('menuitem', {name: 'Edit'})).toBeVisible();
            await expect(promoteMenuItem).toHaveCount(0);
        } finally {
            await page.unrouteAll({behavior: 'ignoreErrors'});

            await deleteProjectDeploymentQuietly(page, developmentDeployment.deploymentId);
        }
    });
});
