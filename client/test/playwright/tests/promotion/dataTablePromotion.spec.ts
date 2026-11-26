import {expect} from '@playwright/test';

import {loginTest, mergeTests, projectTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import {getProjectWorkspaceId} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';
import {
    STAGING_ENVIRONMENT_ID,
    addDataTableColumn,
    countDataTableRows,
    createDataTable,
    dropDataTableQuietly,
    findDataTable,
    goToNextPromotionStep,
    insertDataTableRow,
    openPromotionDialog,
    promoteAndWaitForToast,
    promoteThroughWizard,
    promoteViaApi,
    removeDataTableColumn,
    setCurrentEnvironment,
    skipUnlessPromotionIsAvailable,
} from '../../utils/promotionUtils';

const test = mergeTests(loginTest(), projectTest);

test.describe('data table promotion', () => {
    const dataTableIds: string[] = [];

    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test.afterEach(async ({authenticatedPage}) => {
        for (const dataTableId of dataTableIds.splice(0)) {
            await dropDataTableQuietly(authenticatedPage, STAGING_ENVIRONMENT_ID, dataTableId);
            await dropDataTableQuietly(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID, dataTableId);
        }

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test('creates the table in Staging with the same columns and no rows', async ({
        authenticatedPage: page,
        project,
    }) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const tableName = `table_${getRandomString().slice(0, 8)}`;

        const dataTableId = await createDataTable(page, workspaceId, tableName, [
            {name: 'title', type: 'STRING'},
            {name: 'quantity', type: 'INTEGER'},
        ]);

        dataTableIds.push(dataTableId);

        await insertDataTableRow(page, DEVELOPMENT_ENVIRONMENT_ID, dataTableId, {quantity: 3, title: 'first'});

        await page.goto('/automation/datatables');

        const dialog = await openPromotionDialog(page, tableName);

        await expect(dialog.getByText('Will create')).toBeVisible();

        await goToNextPromotionStep(dialog);

        await expect(dialog.getByText('New table')).toBeVisible();

        await promoteAndWaitForToast(page, dialog);

        const stagingTable = await findDataTable(page, workspaceId, STAGING_ENVIRONMENT_ID, tableName);

        expect(stagingTable?.id).toBe(dataTableId);
        expect(stagingTable?.columns.map((column) => `${column.name}:${column.type}`)).toEqual([
            'title:STRING',
            'quantity:INTEGER',
        ]);

        expect(await countDataTableRows(page, STAGING_ENVIRONMENT_ID, dataTableId)).toBe(0);
    });

    test('warns about a dropped column and removes it from Staging', async ({authenticatedPage: page, project}) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const tableName = `table_${getRandomString().slice(0, 8)}`;

        const dataTableId = await createDataTable(page, workspaceId, tableName, [
            {name: 'title', type: 'STRING'},
            {name: 'quantity', type: 'INTEGER'},
        ]);

        dataTableIds.push(dataTableId);

        await promoteViaApi(page, 'DATA_TABLE', dataTableId);

        const developmentTable = await findDataTable(page, workspaceId, DEVELOPMENT_ENVIRONMENT_ID, tableName);

        const quantityColumn = developmentTable!.columns.find((column) => column.name === 'quantity');

        await removeDataTableColumn(page, DEVELOPMENT_ENVIRONMENT_ID, dataTableId, quantityColumn!.id);

        await page.goto('/automation/datatables');

        const dialog = await openPromotionDialog(page, tableName);

        await goToNextPromotionStep(dialog);

        await expect(dialog.getByText('Deletes data in this column')).toBeVisible();

        await promoteAndWaitForToast(page, dialog);

        const stagingTable = await findDataTable(page, workspaceId, STAGING_ENVIRONMENT_ID, tableName);

        expect(stagingTable?.columns.map((column) => column.name)).toEqual(['title']);
    });

    test('shows the error and leaves Staging unchanged when a retype fails', async ({
        authenticatedPage: page,
        project,
    }) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const tableName = `table_${getRandomString().slice(0, 8)}`;

        const dataTableId = await createDataTable(page, workspaceId, tableName, [
            {name: 'title', type: 'STRING'},
            {name: 'amount', type: 'STRING'},
        ]);

        dataTableIds.push(dataTableId);

        await promoteViaApi(page, 'DATA_TABLE', dataTableId);

        await insertDataTableRow(page, STAGING_ENVIRONMENT_ID, dataTableId, {amount: 'not-a-number', title: 'kept'});

        const developmentTable = await findDataTable(page, workspaceId, DEVELOPMENT_ENVIRONMENT_ID, tableName);

        const amountColumn = developmentTable!.columns.find((column) => column.name === 'amount');

        await removeDataTableColumn(page, DEVELOPMENT_ENVIRONMENT_ID, dataTableId, amountColumn!.id);
        await addDataTableColumn(page, DEVELOPMENT_ENVIRONMENT_ID, dataTableId, {name: 'amount', type: 'INTEGER'});

        await page.goto('/automation/datatables');

        const dialog = await openPromotionDialog(page, tableName);

        await goToNextPromotionStep(dialog);

        await expect(dialog.getByText('Type changes from STRING to INTEGER')).toBeVisible();

        await promoteThroughWizard(dialog);

        await expect(
            page.locator('[data-sonner-toast]').filter({hasText: /Changing column 'amount' of data table/})
        ).toBeVisible({timeout: 30000});

        const stagingTable = await findDataTable(page, workspaceId, STAGING_ENVIRONMENT_ID, tableName);

        expect(stagingTable?.columns.map((column) => `${column.name}:${column.type}`)).toEqual([
            'title:STRING',
            'amount:STRING',
        ]);

        expect(await countDataTableRows(page, STAGING_ENVIRONMENT_ID, dataTableId)).toBe(1);
    });
});
