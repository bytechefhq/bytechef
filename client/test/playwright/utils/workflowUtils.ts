import {Locator, type Page, expect} from '@playwright/test';

import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';

export interface WorkflowDefinitionI {
    tasks?: Array<{
        name?: string;
        parameters?: {
            value?: Record<string, unknown>;
        };
        metadata?: {
            ui?: {
                dynamicPropertyTypes?: Record<string, string>;
            };
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

interface ReplaceMentionsInputValueProps {
    input: Locator;
    page: Page;
    value: string;
}

export async function replaceMentionsInputValue({input, page, value}: ReplaceMentionsInputValueProps): Promise<void> {
    await input.click();

    await page.keyboard.press('Control+a');

    await input.fill(value);
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

interface AddArrayItemViaPopoverProps {
    page: Page;
    arrayProperty: Locator;
    itemType?: string;
}

export async function addArrayItemViaPopover({
    arrayProperty,
    itemType = 'STRING',
    page,
}: AddArrayItemViaPopoverProps): Promise<void> {
    const arrayPopoverTrigger = arrayProperty.getByRole('button', {name: /Add array item/i});
    const arrayPropertyPopover = page.getByLabel('Array property popover');
    const addArraySubpropertyButton = arrayPropertyPopover.getByRole('button', {name: /Add/i});

    if (itemType != null) {
        await expect(async () => {
            if (!(await arrayPropertyPopover.isVisible()) && (await arrayPopoverTrigger.isVisible())) {
                await arrayPopoverTrigger.click();
            }

            await expect(arrayPropertyPopover).toBeVisible({timeout: 100});

            const typeCombobox = arrayPropertyPopover.getByRole('combobox');

            await typeCombobox.click();

            await page.getByRole('option', {name: itemType}).click();

            await addArraySubpropertyButton.click();
        }).toPass();
    } else {
        await clickAndExpectToBeVisible({
            autoClick: true,
            target: addArraySubpropertyButton,
            trigger: arrayPopoverTrigger,
        });
    }
}

interface AddObjectPropertyViaPopoverProps {
    page: Page;
    objectProperty: Locator;
    propertyName: string;
    itemType?: string;
}

export async function addObjectSubPropertyViaPopover({
    itemType = 'STRING',
    objectProperty,
    page,
    propertyName,
}: AddObjectPropertyViaPopoverProps): Promise<void> {
    const objectPopoverTrigger = objectProperty.getByRole('button', {name: /Add object property/i});
    const objectPropertyPopover = page.getByRole('dialog', {name: /property popover/i});

    const propertyNameInput = objectPropertyPopover.getByRole('textbox', {name: /name/i});
    const addObjectSubpropertyButton = objectPropertyPopover.getByRole('button', {name: /Add/i});

    await expect(async () => {
        if (!(await objectPropertyPopover.isVisible()) && (await objectPopoverTrigger.isVisible())) {
            await objectPopoverTrigger.click();
        }

        await expect(objectPropertyPopover).toBeVisible({timeout: 100});

        await propertyNameInput.fill(propertyName);

        if (itemType != null) {
            const typeCombobox = objectPropertyPopover.getByRole('combobox');

            if (await typeCombobox.isVisible()) {
                await typeCombobox.click();

                await page.getByRole('option', {name: itemType}).click();
            }
        }

        await addObjectSubpropertyButton.click();
    }).toPass();
}

export async function openPropertiesTab(componentConfigurationPanel: Locator): Promise<void> {
    const propertiesTabButton = componentConfigurationPanel.getByRole('button', {name: 'Properties'});

    const typeProperty = componentConfigurationPanel.getByLabel('type property');

    await clickAndExpectToBeVisible({
        target: typeProperty,
        trigger: propertiesTabButton,
    });
}
