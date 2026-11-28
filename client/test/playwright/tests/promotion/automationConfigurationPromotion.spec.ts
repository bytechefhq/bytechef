import {expect} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import getRandomString from '../../utils/getRandomString';
import {
    STAGING_ENVIRONMENT_ID,
    buildEmbeddedSlackWorkflowDefinition,
    createAutomationConfiguration,
    createEmbeddedSlackConnection,
    createPublishedAutomationProject,
    deleteAutomationConfigurationQuietly,
    deleteAutomationProjectQuietly,
    deleteEmbeddedConnectionQuietly,
    findAutomationConfiguration,
    getPromotionPreview,
    openPromotionDialog,
    promoteAndWaitForToast,
    setCurrentEnvironment,
    skipUnlessPromotionIsAvailable,
    updateAutomationConfigurationWorkflow,
} from '../../utils/promotionUtils';

const test = loginTest();

test.describe('automation configuration promotion', () => {
    const connectionIds: number[] = [];
    const projectIds: string[] = [];

    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test.afterEach(async ({authenticatedPage}) => {
        for (const projectId of projectIds.splice(0)) {
            for (const environmentId of [STAGING_ENVIRONMENT_ID, DEVELOPMENT_ENVIRONMENT_ID]) {
                const configurations = await findAllConfigurations(authenticatedPage, projectId, environmentId);

                for (const configurationId of configurations) {
                    await deleteAutomationConfigurationQuietly(authenticatedPage, configurationId);
                }
            }

            await deleteAutomationProjectQuietly(authenticatedPage, projectId);
        }

        for (const connectionId of connectionIds.splice(0)) {
            await deleteEmbeddedConnectionQuietly(authenticatedPage, connectionId);
        }

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    async function findAllConfigurations(
        page: Parameters<typeof findAutomationConfiguration>[0],
        projectId: string,
        environmentId: number
    ): Promise<string[]> {
        const configurationName = configurationNamesByProject.get(projectId);

        if (!configurationName) {
            return [];
        }

        const configuration = await findAutomationConfiguration(page, projectId, environmentId, configurationName);

        return configuration ? [configuration.id] : [];
    }

    const configurationNamesByProject = new Map<string, string>();

    async function createSource(page: Parameters<typeof findAutomationConfiguration>[0]) {
        const suffix = getRandomString();

        const connectionName = `connection_${suffix}`;

        const developmentConnectionId = await createEmbeddedSlackConnection(
            page,
            DEVELOPMENT_ENVIRONMENT_ID,
            connectionName
        );
        const stagingConnectionId = await createEmbeddedSlackConnection(page, STAGING_ENVIRONMENT_ID, connectionName);

        connectionIds.push(developmentConnectionId, stagingConnectionId);

        const projectId = await createPublishedAutomationProject(
            page,
            `promotion_${suffix}`,
            buildEmbeddedSlackWorkflowDefinition(`workflow_${suffix}`)
        );

        projectIds.push(projectId);

        const configurationName = `configuration_${suffix}`;

        configurationNamesByProject.set(projectId, configurationName);

        const configurationId = await createAutomationConfiguration(page, projectId, configurationName);

        const developmentConfiguration = await findAutomationConfiguration(
            page,
            projectId,
            DEVELOPMENT_ENVIRONMENT_ID,
            configurationName
        );

        const workflowUuid = developmentConfiguration!.workflows[0].workflowUuid;

        await updateAutomationConfigurationWorkflow(
            page,
            configurationId,
            workflowUuid,
            {slack: developmentConnectionId},
            {greeting: 'development'}
        );

        return {
            configurationId,
            configurationName,
            projectId,
            stagingConnectionId,
            workflowUuid,
        };
    }

    test('creates the configuration disabled in Staging with the same-name connection and empty inputs', async ({
        authenticatedPage: page,
    }) => {
        const {configurationId, configurationName, projectId, stagingConnectionId} = await createSource(page);

        const preview = await getPromotionPreview(page, 'AUTOMATION_CONFIGURATION', configurationId);

        expect(preview.connections[0].suggestedTargetConnectionId).toBe(String(stagingConnectionId));

        await page.goto(`/embedded/automations?projectId=${projectId}&tab=configurations`);

        const dialog = await openPromotionDialog(page, configurationName);

        await expect(dialog.getByText('Will create')).toBeVisible();

        await promoteAndWaitForToast(page, dialog);

        const stagingConfiguration = await findAutomationConfiguration(
            page,
            projectId,
            STAGING_ENVIRONMENT_ID,
            configurationName
        );

        expect(stagingConfiguration).toBeDefined();
        expect(stagingConfiguration?.enabled).toBe(false);

        const stagingWorkflow = stagingConfiguration!.workflows[0];

        expect(stagingWorkflow.providedConnections).toMatchObject({slack: stagingConnectionId});
        expect(Object.keys((stagingWorkflow.providedInputs as Record<string, unknown>) ?? {})).toHaveLength(0);
    });

    test('keeps the Staging inputs when the configuration is promoted again', async ({authenticatedPage: page}) => {
        const {configurationName, projectId, stagingConnectionId, workflowUuid} = await createSource(page);

        await page.goto(`/embedded/automations?projectId=${projectId}&tab=configurations`);

        const firstDialog = await openPromotionDialog(page, configurationName);

        await promoteAndWaitForToast(page, firstDialog);

        const stagingConfiguration = await findAutomationConfiguration(
            page,
            projectId,
            STAGING_ENVIRONMENT_ID,
            configurationName
        );

        await updateAutomationConfigurationWorkflow(
            page,
            stagingConfiguration!.id,
            workflowUuid,
            {slack: stagingConnectionId},
            {greeting: 'staging'}
        );

        await page.goto(`/embedded/automations?projectId=${projectId}&tab=configurations`);

        const secondDialog = await openPromotionDialog(page, configurationName);

        await expect(secondDialog.getByText('Will update')).toBeVisible();

        await promoteAndWaitForToast(page, secondDialog);

        const updatedConfiguration = await findAutomationConfiguration(
            page,
            projectId,
            STAGING_ENVIRONMENT_ID,
            configurationName
        );

        expect(updatedConfiguration?.workflows[0].providedInputs).toMatchObject({greeting: 'staging'});
    });
});
