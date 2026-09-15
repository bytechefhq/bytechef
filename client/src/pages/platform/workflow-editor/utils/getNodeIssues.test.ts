import {describe, expect, it} from 'vitest';

import {WorkflowIssueI, WorkflowIssueKindType} from '../stores/useWorkflowIssuesStore';
import getNodeIssues from './getNodeIssues';

const CLUSTER_ELEMENT_ROOT_NAMES = new Map([
    ['github_1', 'aiAgent_1'],
    ['github_2', 'aiAgent_1'],
    ['openAi_1', 'aiAgent_1'],
]);

const createIssue = (kind: WorkflowIssueKindType, nodeName: string, propertyPath?: string): WorkflowIssueI => ({
    kind,
    message: `${kind} ${nodeName} ${propertyPath ?? ''}`,
    nodeName,
    propertyPath,
    severity: 'ERROR',
    source: 'VALIDATOR',
});

const GITHUB_1_MISSING_OWNER = createIssue('MISSING_REQUIRED', 'aiAgent_1', 'github_1.owner');
const GITHUB_1_MISSING_CONNECTION = createIssue('MISSING_CONNECTION', 'github_1');
const LOGGER_BROKEN_REFERENCE = createIssue('BROKEN_REFERENCE', 'logger_1', 'approval_1.comment');
const ROOT_MISSING_PROMPT = createIssue('MISSING_REQUIRED', 'aiAgent_1', 'prompt');

const ISSUES = [GITHUB_1_MISSING_OWNER, GITHUB_1_MISSING_CONNECTION, LOGGER_BROKEN_REFERENCE, ROOT_MISSING_PROMPT];

describe('getNodeIssues', () => {
    it('returns the issues of a regular task', () => {
        expect(
            getNodeIssues({clusterElementRootNames: CLUSTER_ELEMENT_ROOT_NAMES, issues: ISSUES, nodeName: 'logger_1'})
        ).toEqual([LOGGER_BROKEN_REFERENCE]);
    });

    it('returns the own issues and the cluster element missing connections of an expanded cluster root', () => {
        expect(
            getNodeIssues({clusterElementRootNames: CLUSTER_ELEMENT_ROOT_NAMES, issues: ISSUES, nodeName: 'aiAgent_1'})
        ).toEqual([GITHUB_1_MISSING_CONNECTION, ROOT_MISSING_PROMPT]);
    });

    it('returns every cluster element issue of a collapsed cluster root', () => {
        expect(
            getNodeIssues({
                clusterElementRootNames: CLUSTER_ELEMENT_ROOT_NAMES,
                includeClusterElementIssues: true,
                issues: ISSUES,
                nodeName: 'aiAgent_1',
            })
        ).toEqual([GITHUB_1_MISSING_OWNER, GITHUB_1_MISSING_CONNECTION, ROOT_MISSING_PROMPT]);
    });

    it('returns the property and connection issues of a cluster element', () => {
        expect(
            getNodeIssues({clusterElementRootNames: CLUSTER_ELEMENT_ROOT_NAMES, issues: ISSUES, nodeName: 'github_1'})
        ).toEqual([GITHUB_1_MISSING_OWNER, GITHUB_1_MISSING_CONNECTION]);
    });

    it('returns nothing for a node without issues', () => {
        expect(
            getNodeIssues({clusterElementRootNames: CLUSTER_ELEMENT_ROOT_NAMES, issues: ISSUES, nodeName: 'github_2'})
        ).toEqual([]);
    });
});
