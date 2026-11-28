import {expect} from '@playwright/test';

import {loginTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import getRandomString from '../../utils/getRandomString';
import {
    type EmbeddedIntegrationI,
    STAGING_ENVIRONMENT_ID,
    createIntegrationInstanceConfiguration,
    createPublishedEmbeddedIntegration,
    deleteEmbeddedIntegrationQuietly,
    deleteIntegrationInstanceConfigurationQuietly,
    findIntegrationInstanceConfiguration,
    openPromotionDialog,
    promoteAndWaitForToast,
    setCurrentEnvironment,
    skipUnlessPromotionIsAvailable,
    updateIntegrationInstanceConfigurationInputs,
} from '../../utils/promotionUtils';

const test = loginTest();

test.describe('integration instance configuration promotion', () => {
    const configurationNames: string[] = [];
    const integrationIds: number[] = [];

    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test.afterEach(async ({authenticatedPage}) => {
        for (const configurationName of configurationNames.splice(0)) {
            for (const environmentId of [STAGING_ENVIRONMENT_ID, DEVELOPMENT_ENVIRONMENT_ID]) {
                const configuration = await findIntegrationInstanceConfiguration(
                    authenticatedPage,
                    environmentId,
                    configurationName
                );

                if (configuration) {
                    await deleteIntegrationInstanceConfigurationQuietly(authenticatedPage, configuration.id);
                }
            }
        }

        for (const integrationId of integrationIds.splice(0)) {
            await deleteEmbeddedIntegrationQuietly(authenticatedPage, integrationId);
        }

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    async function createSourceConfiguration(
        page: Parameters<typeof createPublishedEmbeddedIntegration>[0]
    ): Promise<{configurationName: string; integration: EmbeddedIntegrationI}> {
        const suffix = getRandomString();

        const integration = await createPublishedEmbeddedIntegration(page, `promotion_${suffix}`);

        integrationIds.push(integration.id);

        const configurationName = `configuration_${suffix}`;

        configurationNames.push(configurationName);

        await createIntegrationInstanceConfiguration(page, integration, configurationName, {greeting: 'development'});

        return {configurationName, integration};
    }

    test('creates the configuration disabled in Staging with empty settings and inputs', async ({
        authenticatedPage: page,
    }) => {
        const {configurationName, integration} = await createSourceConfiguration(page);

        const developmentConfiguration = await findIntegrationInstanceConfiguration(
            page,
            DEVELOPMENT_ENVIRONMENT_ID,
            configurationName
        );

        expect(developmentConfiguration?.connectionAuthorizationParameters).toMatchObject({
            clientId: 'playwright-client-id',
        });

        await page.goto(`/embedded/integrations?integrationId=${integration.id}&tab=configurations`);

        const dialog = await openPromotionDialog(page, configurationName);

        await expect(dialog.getByText('Will create')).toBeVisible();

        await promoteAndWaitForToast(page, dialog);

        const stagingConfiguration = await findIntegrationInstanceConfiguration(
            page,
            STAGING_ENVIRONMENT_ID,
            configurationName
        );

        expect(stagingConfiguration).toBeDefined();
        expect(stagingConfiguration?.enabled).toBe(false);
        expect(Object.keys(stagingConfiguration?.connectionConnectionParameters ?? {})).toHaveLength(0);
        expect(Object.keys(stagingConfiguration?.connectionAuthorizationParameters ?? {})).toHaveLength(0);
        expect(JSON.stringify(stagingConfiguration)).not.toContain('playwright-client-id');

        const stagingWorkflows = stagingConfiguration?.integrationInstanceConfigurationWorkflows ?? [];

        expect(stagingWorkflows).toHaveLength(integration.workflows.length);
        expect(stagingWorkflows.length).toBeGreaterThan(0);

        for (const workflow of stagingWorkflows) {
            expect(Object.keys(workflow.inputs ?? {})).toHaveLength(0);
        }
    });

    test('keeps the Staging inputs when the configuration is promoted again', async ({authenticatedPage: page}) => {
        const {configurationName, integration} = await createSourceConfiguration(page);

        await page.goto(`/embedded/integrations?integrationId=${integration.id}&tab=configurations`);

        const firstDialog = await openPromotionDialog(page, configurationName);

        await promoteAndWaitForToast(page, firstDialog);

        const stagingConfiguration = await findIntegrationInstanceConfiguration(
            page,
            STAGING_ENVIRONMENT_ID,
            configurationName
        );

        await updateIntegrationInstanceConfigurationInputs(page, stagingConfiguration!.id, {greeting: 'staging'});

        await page.goto(`/embedded/integrations?integrationId=${integration.id}&tab=configurations`);

        const secondDialog = await openPromotionDialog(page, configurationName);

        await expect(secondDialog.getByText('Will update')).toBeVisible();

        await promoteAndWaitForToast(page, secondDialog);

        const updatedConfiguration = await findIntegrationInstanceConfiguration(
            page,
            STAGING_ENVIRONMENT_ID,
            configurationName
        );

        const updatedWorkflows = updatedConfiguration?.integrationInstanceConfigurationWorkflows ?? [];

        expect(updatedWorkflows).toHaveLength(integration.workflows.length);
        expect(updatedWorkflows.length).toBeGreaterThan(0);

        for (const workflow of updatedWorkflows) {
            expect(workflow.inputs).toMatchObject({greeting: 'staging'});
        }
    });
});
