import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowIssuesStore, {
    WorkflowIssueI,
    getWorkflowIssueKey,
    mergeWorkflowIssues,
} from '../useWorkflowIssuesStore';

const issue = (overrides: Partial<WorkflowIssueI>): WorkflowIssueI => ({
    kind: 'MISSING_REQUIRED',
    message: 'm',
    nodeName: 'node_1',
    severity: 'ERROR',
    source: 'VALIDATOR',
    ...overrides,
});

describe('getWorkflowIssueKey', () => {
    it('keys on node, property path and kind, falling back to the message when there is no property path', () => {
        expect(getWorkflowIssueKey(issue({message: 'a', propertyPath: 'table'}))).toBe('node_1|table|MISSING_REQUIRED');
        expect(getWorkflowIssueKey(issue({message: 'b', propertyPath: 'table'}))).toBe('node_1|table|MISSING_REQUIRED');
        expect(getWorkflowIssueKey(issue({propertyPath: undefined}))).toBe('node_1|m|MISSING_REQUIRED');
    });

    it('keeps two unclassified issues on the same node distinct when only their messages differ', () => {
        const firstOtherIssue = issue({kind: 'OTHER', message: 'first problem', propertyPath: undefined});
        const secondOtherIssue = issue({kind: 'OTHER', message: 'second problem', propertyPath: undefined});

        expect(getWorkflowIssueKey(firstOtherIssue)).not.toBe(getWorkflowIssueKey(secondOtherIssue));
    });
});

describe('mergeWorkflowIssues', () => {
    it('collapses identical keys with live over validator over sweep', () => {
        const sweep = issue({
            kind: 'BROKEN_REFERENCE',
            message: 'sweep',
            propertyPath: 'python_1.diff',
            source: 'SWEEP',
        });
        const validator = issue({
            kind: 'BROKEN_REFERENCE',
            message: 'validator',
            propertyPath: 'python_1.diff',
            source: 'VALIDATOR',
        });

        const merged = mergeWorkflowIssues([], [validator], [sweep]);

        expect(merged).toHaveLength(1);
        expect(merged[0].message).toBe('validator');
    });

    it('suppresses a validator MISSING_RESOURCE when a live LOOKUP_FAILED exists for the same property', () => {
        const validator = issue({kind: 'MISSING_RESOURCE', message: 'not found', propertyPath: 'table'});
        const live = issue({kind: 'LOOKUP_FAILED', message: 'no primary key', propertyPath: 'table', source: 'LIVE'});

        const merged = mergeWorkflowIssues([live], [validator], []);

        expect(merged).toHaveLength(1);
        expect(merged[0].kind).toBe('LOOKUP_FAILED');
        expect(merged[0].message).toBe('no primary key');
    });

    it('orders errors before warnings, then by node name', () => {
        const merged = mergeWorkflowIssues(
            [],
            [
                issue({nodeName: 'b_1', severity: 'WARNING'}),
                issue({nodeName: 'z_1', severity: 'ERROR'}),
                issue({nodeName: 'a_1', severity: 'ERROR'}),
            ],
            []
        );

        expect(merged.map((mergedIssue) => mergedIssue.nodeName)).toEqual(['a_1', 'z_1', 'b_1']);
    });

    it('keeps two unclassified issues on the same node when the server could not attribute a property path', () => {
        const firstUnclassifiedIssue = issue({
            kind: 'OTHER',
            message: 'first unclassified problem',
            propertyPath: undefined,
        });
        const secondUnclassifiedIssue = issue({
            kind: 'OTHER',
            message: 'second unclassified problem',
            propertyPath: undefined,
        });

        const merged = mergeWorkflowIssues([], [firstUnclassifiedIssue, secondUnclassifiedIssue], []);

        expect(merged).toHaveLength(2);
        expect(merged.map((mergedIssue) => mergedIssue.message)).toEqual(
            expect.arrayContaining(['first unclassified problem', 'second unclassified problem'])
        );
    });
});

describe('useWorkflowIssuesStore', () => {
    beforeEach(() => {
        useWorkflowIssuesStore.getState().reset();
    });

    it('records, replaces and clears live lookup failures per node and property', () => {
        const {clearLookupFailure, recordLookupFailure} = useWorkflowIssuesStore.getState();

        recordLookupFailure('dataTable_2', 'table', 'first');
        recordLookupFailure('dataTable_2', 'table', 'second');
        recordLookupFailure('dataTable_2', 'id', 'other');

        expect(Object.values(useWorkflowIssuesStore.getState().liveIssues)).toHaveLength(2);
        expect(useWorkflowIssuesStore.getState().liveIssues['dataTable_2|table|LOOKUP_FAILED'].message).toBe('second');

        clearLookupFailure('dataTable_2', 'table');

        expect(Object.keys(useWorkflowIssuesStore.getState().liveIssues)).toEqual(['dataTable_2|id|LOOKUP_FAILED']);
    });

    it('replaces validator and sweep issues wholesale and resets everything', () => {
        const {setIssuesSidebarOpen, setSweepIssues, setValidatorIssues} = useWorkflowIssuesStore.getState();

        setValidatorIssues([issue({nodeName: 'v_1'})]);
        setValidatorIssues([issue({nodeName: 'v_2'})]);
        setSweepIssues([issue({nodeName: 's_1', source: 'SWEEP'})]);
        setIssuesSidebarOpen(true);

        expect(useWorkflowIssuesStore.getState().validatorIssues.map((current) => current.nodeName)).toEqual(['v_2']);

        useWorkflowIssuesStore.getState().reset();

        const state = useWorkflowIssuesStore.getState();

        expect(state.validatorIssues).toEqual([]);
        expect(state.sweepIssues).toEqual([]);
        expect(state.liveIssues).toEqual({});
        expect(state.issuesSidebarOpen).toBe(false);
    });
});
