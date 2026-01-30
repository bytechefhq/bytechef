import {Locator, type Page} from '@playwright/test';

import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';

interface WorkflowDefinitionI {
    tasks?: Array<{
        name?: string;
        parameters?: {
            value?: Record<string, unknown>;
        };
    }>;
}

interface GetPropertyValueProps {
    propertyName: string;
    taskName: string;
    workflowDefinition: WorkflowDefinitionI;
}

export function getPropertyValue({propertyName, taskName, workflowDefinition}: GetPropertyValueProps): unknown {
    const task = workflowDefinition.tasks?.find((taskItem) => taskItem.name === taskName);

    return task?.parameters?.value?.[propertyName];
}

export function formatPropertyValue(value: unknown, propertyName: string): string {
    if (value === null || value === undefined) {
        return '';
    }

    if (typeof value === 'boolean') {
        return value ? 'True' : 'False';
    }

    if (typeof value === 'number') {
        return String(value);
    }

    if (Array.isArray(value)) {
        return String(value[0] ?? '');
    }

    if (typeof value === 'string') {
        if (propertyName === 'DateTime' && value.includes('T')) {
            const dateTimeMatch = value.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?)$/);

            if (dateTimeMatch) {
                return dateTimeMatch[1];
            }
        }

        return value;
    }

    return String(value);
}

export async function getWorkflowDefinition(page: Page, workflowId: string): Promise<WorkflowDefinitionI> {
    await page.waitForTimeout(2000);

    const response = await page.request.get(`/api/automation/internal/workflows/by-project-workflow-id/${workflowId}`);

    const workflow = await response.json();

    return JSON.parse(workflow.definition);
}

export async function reopenConfigurationPanel(page: Page): Promise<Locator> {
    const varNode = page.getByLabel('var_1 node');

    const reloadedConfigurationPanel = page.getByLabel('var_1 component configuration panel');

    await clickAndExpectToBeVisible({
        target: reloadedConfigurationPanel,
        trigger: varNode,
    });

    return reloadedConfigurationPanel;
}

export async function openPropertiesTab(componentConfigurationPanel: Locator): Promise<void> {
    const propertiesTabButton = componentConfigurationPanel.getByRole('button', {name: 'Properties'});

    const typeProperty = componentConfigurationPanel.getByLabel('type property');

    await clickAndExpectToBeVisible({
        target: typeProperty,
        trigger: propertiesTabButton,
    });
}
