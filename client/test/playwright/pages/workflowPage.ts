import {type Locator, type Page, expect} from '@playwright/test';

import sampleWorkflow from '../sampleWorkflow.json';
import {
    type WorkflowDefinitionI,
    addArrayItemViaPopover,
    formatPropertyValue,
    getPropertyValue,
} from '../utils/workflowUtils';

interface ExpectedPropertyValuesI {
    Array: string;
    Boolean: string;
    Date: string;
    DateTime: string;
    Integer: string;
    Number: string;
    String: string;
    Time: string;
}

export class WorkflowPage {
    private static readonly arrayPropertyItemLabelRegex = /Array property item at index \d+/;

    private readonly page: Page;

    static readonly LONG_DEBOUNCE_MS = 700;
    static readonly SHORT_DEBOUNCE_MS = 300;

    static readonly varTask = sampleWorkflow.tasks.find((task) => task.name === 'var_1');

    readonly arrayProperty: Locator;
    readonly arrayPropertyItems: Locator;
    readonly dynamicPropertyTypes?: Record<string, unknown>;
    readonly firstTaskComponentConfigurationPanel: Locator;
    readonly firstNode: Locator;
    readonly objectPropertyParameters?: Record<string, unknown>;
    readonly objectPropertyParameterKeys: string[];
    readonly parentObjectProperty: Locator;
    readonly parentObjectSubPropertyList: Locator;
    readonly workflowDefinition: WorkflowDefinitionI;
    readonly subPropertyListItems: Locator;

    static assertVar1ArrayParameterIsDefined(arrayValue: unknown): void {
        expect(Array.isArray(arrayValue)).toBe(true);
    }

    static getExpectedPropertyValues(
        workflowDefinition: WorkflowDefinitionI,
        taskName: string
    ): ExpectedPropertyValuesI {
        const propertyNames = ['Array', 'Boolean', 'Date', 'DateTime', 'Integer', 'Number', 'String', 'Time'] as const;

        const expectedValues = {} as ExpectedPropertyValuesI;

        for (const propertyName of propertyNames) {
            const propertyValue = getPropertyValue({
                propertyName,
                taskName,
                workflowDefinition,
            });

            expectedValues[propertyName] = formatPropertyValue(propertyValue, propertyName);
        }

        return expectedValues;
    }

    static getFirstTaskArrayParameter(workflowDefinition: WorkflowDefinitionI): unknown {
        return workflowDefinition.tasks?.[0]?.parameters?.value?.Array;
    }

    constructor(page: Page) {
        this.page = page;
        this.firstNode = page.getByLabel('var_1 node');
        this.firstTaskComponentConfigurationPanel = page.getByLabel('var_1 component configuration panel');
        this.arrayProperty = this.firstTaskComponentConfigurationPanel.getByLabel('Array property', {exact: true});

        this.arrayPropertyItems = this.arrayProperty.getByLabel(WorkflowPage.arrayPropertyItemLabelRegex);

        this.parentObjectProperty = this.firstTaskComponentConfigurationPanel.getByLabel('value property');
        this.parentObjectSubPropertyList = this.parentObjectProperty.getByRole('list', {
            name: 'value object properties',
        });

        this.objectPropertyParameters = WorkflowPage.varTask?.parameters?.value as Record<string, unknown> | undefined;

        this.objectPropertyParameterKeys = this.objectPropertyParameters
            ? Object.keys(this.objectPropertyParameters)
            : [];

        this.dynamicPropertyTypes = WorkflowPage.varTask?.metadata?.ui?.dynamicPropertyTypes as
            | Record<string, unknown>
            | undefined;

        this.subPropertyListItems = this.parentObjectSubPropertyList.locator(':scope > li');

        this.workflowDefinition = sampleWorkflow as WorkflowDefinitionI;
    }

    arrayPropertyItemAt(index: number): Locator {
        return this.arrayProperty.getByLabel(`Array property item at index ${index}`);
    }

    arrayPropertyItemTextboxAt(index: number): Locator {
        return this.arrayPropertyItemAt(index).getByRole('textbox');
    }

    async addArrayItemsToReachRowCount({
        itemType = 'STRING',
        targetRowCount,
    }: {
        itemType?: string;
        targetRowCount: number;
    }): Promise<void> {
        while ((await this.arrayPropertyItems.count()) < targetRowCount) {
            await addArrayItemViaPopover({
                arrayProperty: this.arrayProperty,
                itemType,
                page: this.page,
            });

            await this.page.waitForTimeout(WorkflowPage.SHORT_DEBOUNCE_MS);
        }
    }

    async goToWorkflowEditor(projectId: string, workflowId: string): Promise<void> {
        await this.page.goto(`/automation/projects/${projectId}/project-workflows/${workflowId}`);

        await this.page.waitForLoadState('domcontentloaded');

        await this.page.waitForTimeout(2000);
    }
}
