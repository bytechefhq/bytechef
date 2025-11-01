import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const useRun = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {data: workflowTestConfiguration} = useGetWorkflowTestConfigurationQuery({
        environmentId: currentEnvironmentId,
        workflowId: workflow.id!,
    });

    const workflowTestConfigurationConnections = useMemo(
        () =>
            (workflowTestConfiguration?.connections ?? []).reduce(
                (map: {[key: string]: number}, workflowTestConfigurationConnection) => {
                    const {connectionId, workflowConnectionKey, workflowNodeName} = workflowTestConfigurationConnection;

                    map[`${workflowNodeName}_${workflowConnectionKey}`] = connectionId;

                    return map;
                },
                {}
            ),
        [workflowTestConfiguration]
    );

    const workflowTestConfigurationInputs = useMemo(
        () => workflowTestConfiguration?.inputs ?? {},
        [workflowTestConfiguration]
    );

    const runDisabled = useMemo(() => {
        const requiredInputsMissing = (workflow?.inputs ?? []).some(
            (input) => input.required && !workflowTestConfigurationInputs[input.name]
        );

        const noTasks = (workflow?.tasks ?? []).length === 0;

        const requiredConnectionsMissing = (workflow?.tasks ?? [])
            .flatMap((task) => task.connections ?? [])
            .some(
                (workflowConnection) =>
                    workflowConnection.required &&
                    !workflowTestConfigurationConnections[
                        `${workflowConnection.workflowNodeName}_${workflowConnection.key}`
                    ]
            );

        return requiredInputsMissing || noTasks || requiredConnectionsMissing;
    }, [workflow, workflowTestConfigurationInputs, workflowTestConfigurationConnections]);

    return {
        runDisabled,
    };
};
