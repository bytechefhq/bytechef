import {connectionFormSchema} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnectionsPopover';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useConnectionNoteStore} from '@/pages/platform/workflow-editor/stores/useConnectionNoteStore';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkflowDefinitionType, WorkflowTaskType} from '@/shared/types';
import {useState} from 'react';
import {z} from 'zod';
import {useShallow} from 'zustand/react/shallow';

const SPACE = 4;

interface UsePropertyCodeEditorDialogRightPanelConnectionsPropsI {
    componentConnections: ComponentConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}

export const usePropertyCodeEditorDialogRightPanelConnections = ({
    componentConnections,
    workflow,
    workflowNodeName,
}: UsePropertyCodeEditorDialogRightPanelConnectionsPropsI) => {
    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore(
        useShallow((state) => ({
            setShowConnectionNote: state.setShowConnectionNote,
            showConnectionNote: state.showConnectionNote,
        }))
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        ConnectionKeys,
        updateWorkflowMutation,
        useCreateConnectionMutation,
        useGetComponentDefinitionsQuery,
        useGetConnectionTagsQuery,
    } = useWorkflowEditor();

    const handleCloseConnectionNote = () => setShowConnectionNote(false);

    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery({
        environmentId: currentEnvironmentId,
        workflowId: workflow.id!,
        workflowNodeName,
    });

    const handleOnSubmit = (values: z.infer<typeof connectionFormSchema>) => {
        if (!workflow?.definition) {
            return;
        }

        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        const scriptWorkflowTask = workflowDefinition.tasks?.filter((task) => task.name === workflowNodeName)[0];

        if (!scriptWorkflowTask) {
            return;
        }

        workflowDefinition = {
            ...workflowDefinition,
            tasks: [
                ...workflowDefinition.tasks!.map((task) => {
                    if (task.name === workflowNodeName) {
                        return {
                            ...scriptWorkflowTask,
                            connections: {
                                ...(scriptWorkflowTask.connections ?? {}),
                                [values.name]: {
                                    componentName: values.componentName,
                                    componentVersion: values.componentVersion,
                                },
                            },
                        } as WorkflowTaskType;
                    } else {
                        return task;
                    }
                }),
            ],
        };

        updateWorkflowMutation!.mutate({
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        });
    };

    const handleOnRemoveClick = (workflowConnectionKey: string) => {
        if (!workflow?.definition) {
            return;
        }

        let workflowDefinition: WorkflowDefinitionType = JSON.parse(workflow?.definition);

        const scriptWorkflowTask = workflowDefinition.tasks?.filter((task) => task.name === workflowNodeName)[0];

        if (!scriptWorkflowTask) {
            return;
        }

        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const {[workflowConnectionKey]: removed, ...remainingConnections} = scriptWorkflowTask.connections ?? {};

        workflowDefinition = {
            ...workflowDefinition,
            tasks: [
                ...workflowDefinition.tasks!.map((task) => {
                    if (task.name === workflowNodeName) {
                        return {
                            ...scriptWorkflowTask,
                            connections: remainingConnections,
                        } as WorkflowTaskType;
                    } else {
                        return task;
                    }
                }),
            ],
        };

        updateWorkflowMutation!.mutate({
            id: workflow.id!,
            workflow: {
                definition: JSON.stringify(workflowDefinition, null, SPACE),
                version: workflow.version,
            },
        });
    };

    return {
        ConnectionKeys,
        componentConnections,
        componentDefinitions,
        handleCloseConnectionNote,
        handleOnRemoveClick,
        handleOnSubmit,
        setShowNewConnectionDialog,
        showConnectionNote,
        showNewConnectionDialog,
        useCreateConnectionMutation,
        useGetConnectionTagsQuery,
        workflowTestConfigurationConnections,
    };
};
