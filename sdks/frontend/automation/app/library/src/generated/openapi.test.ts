import {describe, expect, it} from 'vitest';

import type {components} from './openapi';

type AppBinding = components['schemas']['AppBinding'];
type AppWorkflow = components['schemas']['AppWorkflow'];
type WorkflowRun = components['schemas']['WorkflowRun'];
type AppTool = components['schemas']['AppTool'];

describe('generated OpenAPI types', () => {
    it('model the nullable fields as unions with null', () => {
        const binding: AppBinding = {
            actionNames: [],
            bindingKey: 'crm',
            componentName: 'hubspot',
            componentVersion: null,
            connectionVersion: 1,
            proxyAllowedMethods: [],
            proxyEnabled: false,
            toolNames: [],
        };
        const workflow: AppWorkflow = {
            available: false,
            description: null,
            inputSchema: null,
            name: 'gone',
            outputSchema: null,
            workflowKey: 'gone',
        };
        const run: WorkflowRun = {
            approvalFormUrl: null,
            error: null,
            result: undefined,
            runId: 'r',
            status: 'RUNNING',
            workflowKey: 'k',
        };
        const tool: AppTool = {
            bindingKey: 'crm',
            componentName: 'hubspot',
            name: 'CRM__SEARCH',
            parameters: '{"type":"object"}',
            toolName: 'search',
        };

        expect(binding.componentVersion).toBeNull();
        expect(workflow.available).toBe(false);
        expect(run.status).toBe('RUNNING');
        expect(typeof tool.parameters).toBe('string');
    });
});
