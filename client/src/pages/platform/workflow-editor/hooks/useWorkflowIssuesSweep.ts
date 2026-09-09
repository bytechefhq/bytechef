import {useEffect} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowIssuesStore from '../stores/useWorkflowIssuesStore';
import collectWorkflowIssues from '../utils/collectWorkflowIssues';

export default function useWorkflowIssuesSweep(): void {
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const setSweepIssues = useWorkflowIssuesStore((state) => state.setSweepIssues);

    useEffect(() => {
        setSweepIssues(
            collectWorkflowIssues({inputs: workflow.inputs, tasks: workflow.tasks, triggers: workflow.triggers})
        );
    }, [setSweepIssues, workflow.inputs, workflow.tasks, workflow.triggers]);
}
