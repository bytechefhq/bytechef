import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type WorkflowIssueKindType =
    | 'BROKEN_REFERENCE'
    | 'DUPLICATE_NODE_NAME'
    | 'LOOKUP_FAILED'
    | 'MISSING_CLUSTER_ELEMENT'
    | 'MISSING_CONNECTION'
    | 'MISSING_REQUIRED'
    | 'MISSING_RESOURCE'
    | 'OTHER'
    | 'TASK_ORDER'
    | 'TYPE_MISMATCH';

export type WorkflowIssueSeverityType = 'ERROR' | 'WARNING';

export type WorkflowIssueSourceType = 'LIVE' | 'SWEEP' | 'VALIDATOR';

export interface WorkflowIssueI {
    kind: WorkflowIssueKindType;
    message: string;
    nodeName: string;
    propertyPath?: string;
    severity: WorkflowIssueSeverityType;
    source: WorkflowIssueSourceType;
}

const SEVERITY_ORDER: Record<WorkflowIssueSeverityType, number> = {ERROR: 0, WARNING: 1};

export function getWorkflowIssueKey({
    kind,
    message,
    nodeName,
    propertyPath,
}: Pick<WorkflowIssueI, 'kind' | 'message' | 'nodeName' | 'propertyPath'>): string {
    return `${nodeName}|${propertyPath ?? message}|${kind}`;
}

export function mergeWorkflowIssues(
    liveIssues: Array<WorkflowIssueI>,
    validatorIssues: Array<WorkflowIssueI>,
    sweepIssues: Array<WorkflowIssueI>
): Array<WorkflowIssueI> {
    const issuesByKey = new Map<string, WorkflowIssueI>();

    for (const currentIssue of [...sweepIssues, ...validatorIssues, ...liveIssues]) {
        issuesByKey.set(getWorkflowIssueKey(currentIssue), currentIssue);
    }

    for (const liveIssue of liveIssues) {
        if (liveIssue.kind === 'LOOKUP_FAILED') {
            issuesByKey.delete(
                getWorkflowIssueKey({
                    kind: 'MISSING_RESOURCE',
                    message: '',
                    nodeName: liveIssue.nodeName,
                    propertyPath: liveIssue.propertyPath,
                })
            );
        }
    }

    return [...issuesByKey.values()].sort(
        (first, second) =>
            SEVERITY_ORDER[first.severity] - SEVERITY_ORDER[second.severity] ||
            first.nodeName.localeCompare(second.nodeName)
    );
}

interface WorkflowIssuesStateI {
    clearLiveIssues: () => void;
    clearLookupFailure: (nodeName: string, propertyPath?: string) => void;
    issuesSidebarOpen: boolean;
    liveIssues: Record<string, WorkflowIssueI>;
    recordLookupFailure: (nodeName: string, propertyPath: string | undefined, message: string) => void;
    reset: () => void;
    setIssuesSidebarOpen: (issuesSidebarOpen: boolean) => void;
    setSweepIssues: (sweepIssues: Array<WorkflowIssueI>) => void;
    setValidatorIssues: (validatorIssues: Array<WorkflowIssueI>) => void;
    sweepIssues: Array<WorkflowIssueI>;
    validatorIssues: Array<WorkflowIssueI>;
}

const initialState = {
    issuesSidebarOpen: false,
    liveIssues: {},
    sweepIssues: [],
    validatorIssues: [],
};

const useWorkflowIssuesStore = create<WorkflowIssuesStateI>()(
    devtools(
        (set) => ({
            ...initialState,
            clearLiveIssues: () => set({liveIssues: {}}),
            clearLookupFailure: (nodeName, propertyPath) =>
                set((state) => {
                    const liveIssues = {...state.liveIssues};

                    delete liveIssues[
                        getWorkflowIssueKey({kind: 'LOOKUP_FAILED', message: '', nodeName, propertyPath})
                    ];

                    return {liveIssues};
                }),
            recordLookupFailure: (nodeName, propertyPath, message) =>
                set((state) => {
                    const liveIssue: WorkflowIssueI = {
                        kind: 'LOOKUP_FAILED',
                        message,
                        nodeName,
                        propertyPath,
                        severity: 'ERROR',
                        source: 'LIVE',
                    };

                    return {liveIssues: {...state.liveIssues, [getWorkflowIssueKey(liveIssue)]: liveIssue}};
                }),
            reset: () => set({...initialState}),
            setIssuesSidebarOpen: (issuesSidebarOpen) => set({issuesSidebarOpen}),
            setSweepIssues: (sweepIssues) => set({sweepIssues}),
            setValidatorIssues: (validatorIssues) => set({validatorIssues}),
        }),
        {name: 'workflow-issues'}
    )
);

export default useWorkflowIssuesStore;
