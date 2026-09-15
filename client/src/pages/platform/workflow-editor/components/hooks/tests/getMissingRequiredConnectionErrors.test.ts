import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getMissingRequiredConnectionErrors, {getWorkflowIssueErrors} from '../getMissingRequiredConnectionErrors';

const createConnection = (
    componentName: string,
    key: string,
    workflowNodeName: string,
    required = true
): ComponentConnection => ({
    componentName,
    componentVersion: 1,
    key,
    required,
    workflowNodeName,
});

const createTestConfigurationConnection = (workflowConnectionKey: string): WorkflowTestConfigurationConnection => ({
    connectionId: 1,
    workflowConnectionKey,
    workflowNodeName: 'aiAgent_1',
});

describe('getMissingRequiredConnectionErrors', () => {
    it('reports a required task connection without a selected connection using the component title', () => {
        expect(
            getMissingRequiredConnectionErrors({
                componentTitle: 'GitHub',
                connections: [createConnection('github', 'github', 'github_4')],
                workflowTestConfigurationConnections: [],
            })
        ).toEqual([{kind: 'CONNECTION', name: 'GitHub'}]);
    });

    it('does not report a required connection that has a selected connection', () => {
        expect(
            getMissingRequiredConnectionErrors({
                componentTitle: 'GitHub',
                connections: [createConnection('github', 'github', 'github_4')],
                workflowTestConfigurationConnections: [createTestConfigurationConnection('github')],
            })
        ).toEqual([]);
    });

    it('does not report optional connections', () => {
        expect(
            getMissingRequiredConnectionErrors({
                componentTitle: 'HTTP Client',
                connections: [createConnection('httpClient', 'httpClient', 'httpClient_1', false)],
                workflowTestConfigurationConnections: [],
            })
        ).toEqual([]);
    });

    it('reports a required task connection with a custom key using the component title', () => {
        expect(
            getMissingRequiredConnectionErrors({
                componentTitle: 'JavaScript',
                connections: [createConnection('googleMail', 'javascript01', 'script_1')],
                workflowTestConfigurationConnections: [],
            })
        ).toEqual([{kind: 'CONNECTION', name: 'JavaScript'}]);
    });

    it('reports each missing cluster element connection on a cluster root by cluster element name', () => {
        expect(
            getMissingRequiredConnectionErrors({
                clusterRoot: true,
                componentTitle: 'AI Agent',
                connections: [
                    createConnection('openAi', 'openAi_1', 'aiAgent_1'),
                    createConnection('github', 'github_1', 'aiAgent_1'),
                    createConnection('github', 'github_2', 'aiAgent_1'),
                ],
                workflowTestConfigurationConnections: [createTestConfigurationConnection('openAi_1')],
            })
        ).toEqual([
            {kind: 'CONNECTION', name: 'github_1'},
            {kind: 'CONNECTION', name: 'github_2'},
        ]);
    });

    it('reports the missing own connection of a cluster root using the component title', () => {
        expect(
            getMissingRequiredConnectionErrors({
                clusterRoot: true,
                componentTitle: 'Vector Store',
                connections: [createConnection('pgVector', 'pgVector', 'vectorStore_1')],
                workflowTestConfigurationConnections: [],
            })
        ).toEqual([{kind: 'CONNECTION', name: 'Vector Store'}]);
    });

    it('reports nothing while the selected connections are not loaded', () => {
        expect(
            getMissingRequiredConnectionErrors({
                componentTitle: 'GitHub',
                connections: [createConnection('github', 'github', 'github_4')],
                workflowTestConfigurationConnections: undefined,
            })
        ).toEqual([]);
    });
});

describe('getWorkflowIssueErrors', () => {
    it('lists broken references and other issue kinds by their message', () => {
        expect(
            getWorkflowIssueErrors([
                {
                    kind: 'BROKEN_REFERENCE',
                    message: '"approval_1" is missing from the workflow (referenced as approval_1.comment)',
                },
                {kind: 'LOOKUP_FAILED', message: 'Request failed with status 500'},
            ])
        ).toEqual([
            {kind: 'ISSUE', name: '"approval_1" is missing from the workflow (referenced as approval_1.comment)'},
            {kind: 'ISSUE', name: 'Request failed with status 500'},
        ]);
    });

    it('skips missing required properties and connections that are listed separately', () => {
        expect(
            getWorkflowIssueErrors([
                {kind: 'MISSING_REQUIRED', message: 'Missing required property: owner'},
                {kind: 'MISSING_CONNECTION', message: 'Missing required connection: GitHub'},
            ])
        ).toEqual([]);
    });

    it('lists an issue reported by several sources once', () => {
        expect(
            getWorkflowIssueErrors([
                {kind: 'TYPE_MISMATCH', message: "Property 'count' has incorrect type."},
                {kind: 'TYPE_MISMATCH', message: "Property 'count' has incorrect type."},
            ])
        ).toEqual([{kind: 'ISSUE', name: "Property 'count' has incorrect type."}]);
    });
});
