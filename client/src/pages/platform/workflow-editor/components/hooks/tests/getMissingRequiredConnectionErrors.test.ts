import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getMissingRequiredConnectionErrors, {
    getWorkflowIssueErrors,
    getWorkflowNodeDetailsErrorsSummary,
} from '../getMissingRequiredConnectionErrors';

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
        ).toEqual([{kind: 'CONNECTION', name: 'GitHub', severity: 'ERROR'}]);
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
        ).toEqual([{kind: 'CONNECTION', name: 'JavaScript', severity: 'ERROR'}]);
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
            {kind: 'CONNECTION', name: 'github_1', severity: 'ERROR'},
            {kind: 'CONNECTION', name: 'github_2', severity: 'ERROR'},
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
        ).toEqual([{kind: 'CONNECTION', name: 'Vector Store', severity: 'ERROR'}]);
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
                    severity: 'ERROR',
                },
                {kind: 'LOOKUP_FAILED', message: 'Request failed with status 500', severity: 'ERROR'},
            ])
        ).toEqual([
            {
                kind: 'ISSUE',
                name: '"approval_1" is missing from the workflow (referenced as approval_1.comment)',
                severity: 'ERROR',
            },
            {kind: 'ISSUE', name: 'Request failed with status 500', severity: 'ERROR'},
        ]);
    });

    it('skips missing required properties and connections that are listed separately', () => {
        expect(
            getWorkflowIssueErrors([
                {kind: 'MISSING_REQUIRED', message: 'Missing required property: owner', severity: 'ERROR'},
                {kind: 'MISSING_CONNECTION', message: 'Missing required connection: GitHub', severity: 'ERROR'},
            ])
        ).toEqual([]);
    });

    it('keeps a missing recommended field warning, which is not listed separately', () => {
        expect(
            getWorkflowIssueErrors([
                {kind: 'MISSING_REQUIRED', message: 'Missing recommended field: label', severity: 'WARNING'},
            ])
        ).toEqual([{kind: 'ISSUE', name: 'Missing recommended field: label', severity: 'WARNING'}]);
    });

    it('lists an issue reported by several sources once', () => {
        expect(
            getWorkflowIssueErrors([
                {kind: 'TYPE_MISMATCH', message: "Property 'count' has incorrect type.", severity: 'ERROR'},
                {kind: 'TYPE_MISMATCH', message: "Property 'count' has incorrect type.", severity: 'ERROR'},
            ])
        ).toEqual([{kind: 'ISSUE', name: "Property 'count' has incorrect type.", severity: 'ERROR'}]);
    });

    it('names the properties an issue concerns, and keeps the same message on different properties apart', () => {
        const typeMismatch = {
            kind: 'TYPE_MISMATCH' as const,
            message: 'Property is of type array, not integer',
            propertyPath: 'firecrawl_5.data.json.result',
            severity: 'ERROR' as const,
        };

        expect(
            getWorkflowIssueErrors(
                [
                    typeMismatch,
                    {...typeMismatch, propertyPath: 'firecrawl_6.data.json.result'},
                    {kind: 'OTHER', message: 'Deprecated option', severity: 'WARNING'},
                ],
                (nodeIssue) =>
                    nodeIssue.propertyPath === 'firecrawl_6.data.json.result' ? ['Temperature'] : ['Top K', 'Top K']
            )
        ).toEqual([
            {kind: 'ISSUE', name: 'Property is of type array, not integer', propertyLabel: 'Top K', severity: 'ERROR'},
            {
                kind: 'ISSUE',
                name: 'Property is of type array, not integer',
                propertyLabel: 'Temperature',
                severity: 'ERROR',
            },
            {kind: 'ISSUE', name: 'Deprecated option', propertyLabel: 'Top K', severity: 'WARNING'},
        ]);
    });

    it('keeps the severity of a warning', () => {
        expect(getWorkflowIssueErrors([{kind: 'OTHER', message: 'Deprecated operation', severity: 'WARNING'}])).toEqual(
            [{kind: 'ISSUE', name: 'Deprecated operation', severity: 'WARNING'}]
        );
    });

    it('lists a message reported as both an error and a warning once, as an error', () => {
        expect(
            getWorkflowIssueErrors([
                {kind: 'OTHER', message: 'Something is off', severity: 'WARNING'},
                {kind: 'OTHER', message: 'Something is off', severity: 'ERROR'},
                {kind: 'OTHER', message: 'Something is off', severity: 'WARNING'},
            ])
        ).toEqual([{kind: 'ISSUE', name: 'Something is off', severity: 'ERROR'}]);
    });
});

describe('getWorkflowNodeDetailsErrorsSummary', () => {
    it('titles a list of errors as errors', () => {
        expect(getWorkflowNodeDetailsErrorsSummary([{severity: 'ERROR'}, {severity: 'ERROR'}])).toEqual({
            heading: 'Errors (2)',
            warningOnly: false,
        });
    });

    it('titles a list of warnings as warnings and marks it warning-only', () => {
        expect(getWorkflowNodeDetailsErrorsSummary([{severity: 'WARNING'}])).toEqual({
            heading: 'Warnings (1)',
            warningOnly: true,
        });
    });

    it('counts errors and warnings separately when both are present', () => {
        expect(
            getWorkflowNodeDetailsErrorsSummary([{severity: 'WARNING'}, {severity: 'ERROR'}, {severity: 'WARNING'}])
        ).toEqual({heading: 'Errors (1), Warnings (2)', warningOnly: false});
    });

    it('is not warning-only when there is nothing to show', () => {
        expect(getWorkflowNodeDetailsErrorsSummary([])).toEqual({heading: '', warningOnly: false});
    });
});
