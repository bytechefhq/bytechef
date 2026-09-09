import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowIssuesStore, {WorkflowIssueI, mergeWorkflowIssues} from '../stores/useWorkflowIssuesStore';

export default function useWorkflowIssues(): Array<WorkflowIssueI> {
    const {liveIssues, sweepIssues, validatorIssues} = useWorkflowIssuesStore(
        useShallow((state) => ({
            liveIssues: state.liveIssues,
            sweepIssues: state.sweepIssues,
            validatorIssues: state.validatorIssues,
        }))
    );

    return useMemo(
        () => mergeWorkflowIssues(Object.values(liveIssues), validatorIssues, sweepIssues),
        [liveIssues, sweepIssues, validatorIssues]
    );
}
