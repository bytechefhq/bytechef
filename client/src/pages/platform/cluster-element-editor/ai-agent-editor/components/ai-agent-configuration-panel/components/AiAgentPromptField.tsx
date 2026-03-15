import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

interface AiAgentPromptFieldProps {
    componentDefinitions: ComponentDefinitionBasic[];
    containerClassName?: string;
    dataPills: DataPillType[];
    editorClassName?: string;
    path: string;
    placeholder: string;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    title: string;
    workflow: Workflow;
}

export default function AiAgentPromptField({
    componentDefinitions,
    containerClassName,
    dataPills,
    editorClassName,
    path,
    placeholder,
    taskDispatcherDefinitions,
    title,
    workflow,
}: AiAgentPromptFieldProps) {
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const parameterValue = useMemo(() => {
        if (!workflow.definition || !rootClusterElementNodeData?.workflowNodeName) {
            return undefined;
        }

        try {
            const definition = JSON.parse(workflow.definition);
            const tasks = definition.tasks || [];
            const rootTask = tasks.find(
                (task: {name: string}) => task.name === rootClusterElementNodeData.workflowNodeName
            );

            return rootTask?.parameters?.[path] as string | undefined;
        } catch {
            return undefined;
        }
    }, [path, rootClusterElementNodeData?.workflowNodeName, workflow.definition]);

    return (
        <div className={twMerge('flex flex-col', containerClassName)}>
            <h2 className="mb-2">{title}</h2>

            <div
                className={twMerge(
                    'property-mentions-editor overflow-y-auto rounded-lg border border-muted p-3',
                    editorClassName
                )}
            >
                <PropertyMentionsInputEditor
                    className="size-full"
                    componentDefinitions={componentDefinitions}
                    controlType="TEXT_AREA"
                    dataPills={dataPills}
                    path={path}
                    placeholder={placeholder}
                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                    type="STRING"
                    value={parameterValue}
                    workflow={workflow}
                />
            </div>
        </div>
    );
}
