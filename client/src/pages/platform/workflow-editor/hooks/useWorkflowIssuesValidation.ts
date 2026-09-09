import {useValidateWorkflowQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowIssuesStore, {WorkflowIssueI} from '../stores/useWorkflowIssuesStore';

export default function useWorkflowIssuesValidation(): void {
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {clearLiveIssues, setValidatorIssues} = useWorkflowIssuesStore(
        useShallow((state) => ({
            clearLiveIssues: state.clearLiveIssues,
            setValidatorIssues: state.setValidatorIssues,
        }))
    );

    const {data} = useValidateWorkflowQuery(
        {environmentId: currentEnvironmentId, workflowDefinition: workflow.definition!},
        {enabled: !!workflow.definition}
    );

    useEffect(() => {
        clearLiveIssues();
        setValidatorIssues([]);
    }, [clearLiveIssues, setValidatorIssues, workflow.definition]);

    useEffect(() => {
        if (!data) {
            return;
        }

        const validatorIssues: Array<WorkflowIssueI> = data.validateWorkflow.nodeIssues.map((nodeIssue) => ({
            kind: nodeIssue.kind,
            message: nodeIssue.message,
            nodeName: nodeIssue.nodeName,
            propertyPath: nodeIssue.propertyPath ?? undefined,
            severity: nodeIssue.severity,
            source: 'VALIDATOR',
        }));

        setValidatorIssues(validatorIssues);
    }, [data, setValidatorIssues]);
}
