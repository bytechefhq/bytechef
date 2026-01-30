import {Page} from '@playwright/test';

import {formatPropertyValue, getPropertyValue} from '../utils/workflowUtils';

interface WorkflowDefinitionI {
    tasks?: Array<{
        name?: string;
        parameters?: {
            value?: Record<string, unknown>;
        };
    }>;
}

interface ExpectedPropertyValues {
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
    constructor(private readonly page: Page) {}

    async goToWorkflowEditor(projectId: string, workflowId: string): Promise<void> {
        await this.page.goto(`/automation/projects/${projectId}/project-workflows/${workflowId}`);

        await this.page.waitForLoadState('domcontentloaded');

        await this.page.waitForTimeout(2000);
    }

    static getExpectedPropertyValues(
        workflowDefinition: WorkflowDefinitionI,
        taskName: string
    ): ExpectedPropertyValues {
        const propertyNames = ['Array', 'Boolean', 'Date', 'DateTime', 'Integer', 'Number', 'String', 'Time'] as const;

        const expectedValues = {} as ExpectedPropertyValues;

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
}
