import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export interface RunDisabledReasonI {
    key: string;
    message: string;
}

export const useRun = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);
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

    const runDisabledReasons = useMemo(() => {
        const reasons: RunDisabledReasonI[] = [];

        const tasks = workflow?.tasks ?? [];

        if (tasks.length === 0) {
            reasons.push({key: 'tasks', message: 'Add at least one task.'});
        }

        (workflow?.inputs ?? [])
            .filter((input) => input.required && !workflowTestConfigurationInputs[input.name])
            .forEach((input) =>
                reasons.push({
                    key: `input_${input.name}`,
                    message: `Set a test value for the "${input.label || input.name}" input.`,
                })
            );

        tasks.forEach((task) =>
            (task.connections ?? [])
                .filter(
                    (workflowConnection) =>
                        workflowConnection.required &&
                        !workflowTestConfigurationConnections[
                            `${workflowConnection.workflowNodeName}_${workflowConnection.key}`
                        ]
                )
                .forEach((workflowConnection) => {
                    const componentTitle =
                        componentDefinitions.find(
                            (componentDefinition) => componentDefinition.name === workflowConnection.componentName
                        )?.title || workflowConnection.componentName;

                    const connectionName =
                        workflowConnection.key === workflowConnection.componentName
                            ? ''
                            : ` ("${workflowConnection.key}")`;

                    reasons.push({
                        key: `connection_${workflowConnection.workflowNodeName}_${workflowConnection.key}`,
                        message: `Pick a ${componentTitle} connection${connectionName} for "${task.label || task.name}".`,
                    });
                })
        );

        return reasons;
    }, [componentDefinitions, workflow, workflowTestConfigurationInputs, workflowTestConfigurationConnections]);

    const runDisabled = runDisabledReasons.length > 0;

    return {
        runDisabled,
        runDisabledReasons,
    };
};
