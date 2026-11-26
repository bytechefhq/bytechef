import {expect} from '@playwright/test';

import {loginTest, mergeTests, projectTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import {getProjectWorkspaceId} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';
import {
    STAGING_ENVIRONMENT_ID,
    STAGING_LABEL,
    buildPlainWorkflow,
    buildSlackWorkflow,
    createDevelopmentDeployment,
    createSlackConnection,
    deleteConnectionQuietly,
    deleteProjectDeploymentQuietly,
    getCurrentEnvironmentId,
    getProjectDeployment,
    getPromotionPreview,
    getWorkspaceProjectDeployments,
    goToNextPromotionStep,
    openPromotionDialog,
    promoteAndWaitForToast,
    promoteViaApi,
    setCurrentEnvironment,
    skipUnlessPromotionIsAvailable,
} from '../../utils/promotionUtils';

const test = mergeTests(loginTest(), projectTest);

const connectionIds: number[] = [];

test.describe('Project deployment promotion', () => {
    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test.afterEach(async ({authenticatedPage, project}) => {
        const workspaceId = await getProjectWorkspaceId(authenticatedPage, project.id);

        for (const environmentId of [STAGING_ENVIRONMENT_ID, DEVELOPMENT_ENVIRONMENT_ID]) {
            const deployments = await getWorkspaceProjectDeployments(
                authenticatedPage,
                workspaceId,
                project.id,
                environmentId
            );

            for (const deployment of deployments) {
                await deleteProjectDeploymentQuietly(authenticatedPage, deployment.id);
            }
        }

        for (const connectionId of connectionIds.splice(0)) {
            await deleteConnectionQuietly(authenticatedPage, connectionId);
        }

        await authenticatedPage.goto('/');

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test('creates a disabled Staging copy on the same project version and opens it from the toast', async ({
        authenticatedPage: page,
        project,
    }) => {
        const developmentDeployment = await createDevelopmentDeployment({
            page,
            projectId: project.id,
            workflow: buildPlainWorkflow(`workflow_${getRandomString()}`),
        });

        await page.goto(`/automation/projects?projectId=${project.id}&tab=deployments`);

        const dialog = await openPromotionDialog(page, developmentDeployment.deploymentName);

        await expect(dialog.getByText('Will create')).toBeVisible();

        await goToNextPromotionStep(dialog);

        await expect(dialog.getByText(`${project.name}: v${developmentDeployment.projectVersion} → new`)).toBeVisible();

        const toast = await promoteAndWaitForToast(page, dialog);

        await expect(toast).toContainText(`was created disabled in ${STAGING_LABEL}`);

        const preview = await getPromotionPreview(page, 'PROJECT_DEPLOYMENT', developmentDeployment.deploymentId);

        expect(preview.existingTargetId).not.toBeNull();

        const stagingDeployment = await getProjectDeployment(page, preview.existingTargetId!);

        expect(stagingDeployment.enabled).toBe(false);
        expect(stagingDeployment.projectVersion).toBe(developmentDeployment.projectVersion);

        await toast.getByRole('button', {name: `View in ${STAGING_LABEL}`}).click();

        await expect(page).toHaveURL(new RegExp(`/automation/deployments\\?projectId=${project.id}$`));

        expect(await getCurrentEnvironmentId(page)).toBe(STAGING_ENVIRONMENT_ID);

        await expect(page.getByText(stagingDeployment.name, {exact: true})).toBeVisible();
        await expect(
            page.locator('div.cursor-pointer').filter({hasText: stagingDeployment.name}).getByRole('switch').first()
        ).not.toBeChecked();
    });

    test('updates the existing Staging copy instead of creating a second one', async ({
        authenticatedPage: page,
        project,
    }) => {
        const developmentDeployment = await createDevelopmentDeployment({
            page,
            projectId: project.id,
            workflow: buildPlainWorkflow(`workflow_${getRandomString()}`),
        });

        const firstPromotion = await promoteViaApi(page, 'PROJECT_DEPLOYMENT', developmentDeployment.deploymentId);

        expect(firstPromotion.errors).toBeUndefined();

        const stagingDeploymentId = firstPromotion.data!.promoteToEnvironment.targetId;

        const stagingDeployment = await getProjectDeployment(page, stagingDeploymentId);

        await page.goto(`/automation/projects?projectId=${project.id}&tab=deployments`);

        const dialog = await openPromotionDialog(page, developmentDeployment.deploymentName);

        await expect(dialog.getByText(`Will update ${stagingDeployment.name}`)).toBeVisible();

        const toast = await promoteAndWaitForToast(page, dialog);

        await expect(toast).toContainText(`The existing "${developmentDeployment.deploymentName}" counterpart`);
        await expect(toast).toContainText('was updated');

        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const stagingDeployments = await getWorkspaceProjectDeployments(
            page,
            workspaceId,
            project.id,
            STAGING_ENVIRONMENT_ID
        );

        expect(stagingDeployments.map((deployment) => String(deployment.id))).toEqual([stagingDeploymentId]);
    });

    test.describe('connections', () => {
        test('leaves a connection without a Staging counterpart unresolved', async ({
            authenticatedPage: page,
            project,
        }) => {
            const workspaceId = await getProjectWorkspaceId(page, project.id);

            const connectionName = `playwright-slack-${getRandomString()}`;

            const developmentConnectionId = await createSlackConnection({
                environmentId: DEVELOPMENT_ENVIRONMENT_ID,
                name: connectionName,
                page,
                workspaceId,
            });

            connectionIds.push(developmentConnectionId);

            const developmentDeployment = await createDevelopmentDeployment({
                connectionId: developmentConnectionId,
                page,
                projectId: project.id,
                workflow: buildSlackWorkflow(`workflow_${getRandomString()}`),
            });

            await page.goto(`/automation/projects?projectId=${project.id}&tab=deployments`);

            const dialog = await openPromotionDialog(page, developmentDeployment.deploymentName);

            await goToNextPromotionStep(dialog);
            await goToNextPromotionStep(dialog);

            const connectionSelect = dialog.getByRole('combobox', {name: `Target connection for ${connectionName}`});

            await expect(connectionSelect).toHaveText('Unresolved');
            await expect(dialog.getByText('Unbound connections')).toBeVisible();

            await connectionSelect.click();

            await expect(page.getByRole('option', {name: 'Unresolved'})).toBeVisible();
            await expect(page.getByRole('option', {name: connectionName})).toHaveCount(0);

            await page.keyboard.press('Escape');

            const toast = await promoteAndWaitForToast(page, dialog);

            await expect(toast).toContainText('1 connections still need a binding.');
        });

        test('preselects and binds the same-name Staging connection', async ({authenticatedPage: page, project}) => {
            const workspaceId = await getProjectWorkspaceId(page, project.id);

            const connectionName = `playwright-slack-${getRandomString()}`;

            const developmentConnectionId = await createSlackConnection({
                environmentId: DEVELOPMENT_ENVIRONMENT_ID,
                name: connectionName,
                page,
                workspaceId,
            });

            const stagingConnectionId = await createSlackConnection({
                environmentId: STAGING_ENVIRONMENT_ID,
                name: connectionName,
                page,
                workspaceId,
            });

            connectionIds.push(developmentConnectionId, stagingConnectionId);

            const developmentDeployment = await createDevelopmentDeployment({
                connectionId: developmentConnectionId,
                page,
                projectId: project.id,
                workflow: buildSlackWorkflow(`workflow_${getRandomString()}`),
            });

            await page.goto(`/automation/projects?projectId=${project.id}&tab=deployments`);

            const dialog = await openPromotionDialog(page, developmentDeployment.deploymentName);

            await goToNextPromotionStep(dialog);
            await goToNextPromotionStep(dialog);

            await expect(dialog.getByRole('combobox', {name: `Target connection for ${connectionName}`})).toHaveText(
                connectionName
            );
            await expect(dialog.getByText('Unbound connections')).toBeHidden();

            const toast = await promoteAndWaitForToast(page, dialog);

            await expect(toast).not.toContainText('still need a binding');

            const preview = await getPromotionPreview(page, 'PROJECT_DEPLOYMENT', developmentDeployment.deploymentId);

            const stagingDeployment = await getProjectDeployment(page, preview.existingTargetId!);

            const boundConnectionIds = stagingDeployment.projectDeploymentWorkflows.flatMap((deploymentWorkflow) =>
                (deploymentWorkflow.connections ?? []).map((connection) => connection.connectionId)
            );

            expect(boundConnectionIds).toEqual([stagingConnectionId]);
        });
    });
});
