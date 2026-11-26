import {expect} from '@playwright/test';

import {loginTest, mergeTests, projectTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import {getProjectWorkspaceId, importWorkflowDefinition, publishProject} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';
import {
    STAGING_ENVIRONMENT_ID,
    STAGING_LABEL,
    buildPlainWorkflow,
    createApiCollection,
    deleteApiCollectionQuietly,
    getApiCollection,
    getCurrentEnvironmentId,
    getPromotionPreview,
    openPromotionDialog,
    promoteAndWaitForToast,
    setCurrentEnvironment,
    skipUnlessPromotionIsAvailable,
} from '../../utils/promotionUtils';

const test = mergeTests(loginTest(), projectTest);

test.describe('API collection promotion', () => {
    const apiCollectionIds: string[] = [];

    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test.afterEach(async ({authenticatedPage}) => {
        for (const apiCollectionId of apiCollectionIds.splice(0)) {
            await deleteApiCollectionQuietly(authenticatedPage, apiCollectionId);
        }

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test('creates a disabled Staging copy, then updates it on the next promotion', async ({
        authenticatedPage: page,
        project,
    }) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        await importWorkflowDefinition(page, project.id, buildPlainWorkflow(`workflow_${getRandomString()}`));

        const projectVersion = await publishProject(page, project.id);

        const apiCollectionName = `api_collection_${getRandomString()}`;

        const developmentApiCollectionId = await createApiCollection({
            name: apiCollectionName,
            page,
            projectId: project.id,
            projectVersion,
            workspaceId,
        });

        apiCollectionIds.push(String(developmentApiCollectionId));

        await page.goto('/automation/api-platform/api-collections');

        const createDialog = await openPromotionDialog(page, apiCollectionName);

        await expect(createDialog.getByText('Will create')).toBeVisible();

        const createToast = await promoteAndWaitForToast(page, createDialog);

        const preview = await getPromotionPreview(page, 'API_COLLECTION', developmentApiCollectionId);

        const stagingApiCollectionId = preview.existingTargetId!;

        apiCollectionIds.unshift(stagingApiCollectionId);

        await expect(createToast).toContainText(`was created disabled in ${STAGING_LABEL}`);

        expect((await getApiCollection(page, stagingApiCollectionId)).enabled).toBe(false);

        await page.reload();

        const updateDialog = await openPromotionDialog(page, apiCollectionName);

        await expect(updateDialog.getByText(`Will update ${preview.existingTargetName}`)).toBeVisible();

        const updateToast = await promoteAndWaitForToast(page, updateDialog);

        await expect(updateToast).toContainText('was updated');

        expect((await getPromotionPreview(page, 'API_COLLECTION', developmentApiCollectionId)).existingTargetId).toBe(
            stagingApiCollectionId
        );

        await updateToast.getByRole('button', {name: `View in ${STAGING_LABEL}`}).click();

        await expect(page).toHaveURL(/\/automation\/api-platform\/api-collections$/);

        expect(await getCurrentEnvironmentId(page)).toBe(STAGING_ENVIRONMENT_ID);

        await expect(page.getByText(apiCollectionName, {exact: true})).toBeVisible();
    });
});
