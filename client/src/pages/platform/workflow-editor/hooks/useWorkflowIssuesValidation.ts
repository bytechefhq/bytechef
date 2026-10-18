import {useValidateWorkflowQuery} from '@/shared/middleware/graphql';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowIssuesStore, {WorkflowIssueI} from '../stores/useWorkflowIssuesStore';
import invalidateWorkflowValidation from '../utils/invalidateWorkflowValidation';

export default function useWorkflowIssuesValidation(): void {
    const previousTestConfigurationConnectionsKeyRef = useRef<string | undefined>(undefined);

    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {clearLiveIssues, setValidatorIssues} = useWorkflowIssuesStore(
        useShallow((state) => ({
            clearLiveIssues: state.clearLiveIssues,
            setValidatorIssues: state.setValidatorIssues,
        }))
    );

    const {data} = useValidateWorkflowQuery(
        {environmentId: currentEnvironmentId, workflowDefinition: workflow.definition!, workflowId: workflow.id},
        {enabled: !!workflow.definition}
    );

    const {data: workflowTestConfiguration, isPending: workflowTestConfigurationPending} =
        useGetWorkflowTestConfigurationQuery(
            {environmentId: currentEnvironmentId, workflowId: workflow.id!},
            !!workflow.id
        );

    const queryClient = useQueryClient();

    const testConfigurationConnectionsKey = useMemo(() => {
        if (workflowTestConfigurationPending) {
            return undefined;
        }

        return (workflowTestConfiguration?.connections ?? [])
            .map(
                (connection) =>
                    `${connection.workflowNodeName}|${connection.workflowConnectionKey}|${connection.connectionId}`
            )
            .sort()
            .join(',');
    }, [workflowTestConfiguration?.connections, workflowTestConfigurationPending]);

    useEffect(() => {
        clearLiveIssues();
    }, [clearLiveIssues, workflow.definition]);

    useEffect(() => {
        previousTestConfigurationConnectionsKeyRef.current = undefined;

        setValidatorIssues([]);
    }, [setValidatorIssues, workflow.id]);

    useEffect(() => {
        if (testConfigurationConnectionsKey === undefined) {
            return;
        }

        const previousTestConfigurationConnectionsKey = previousTestConfigurationConnectionsKeyRef.current;

        previousTestConfigurationConnectionsKeyRef.current = testConfigurationConnectionsKey;

        if (
            previousTestConfigurationConnectionsKey !== undefined &&
            previousTestConfigurationConnectionsKey !== testConfigurationConnectionsKey
        ) {
            invalidateWorkflowValidation(queryClient);
        }
    }, [queryClient, testConfigurationConnectionsKey]);

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
