import AiAgentModelSelectField from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentModelSelectField';
import AiAgentPromptField from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentPromptField';
import AiAgentSkills from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentSkills';
import AiAgentStreamResponseField from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentStreamResponseField';
import AiAgentTools from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentTools';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useShallow} from 'zustand/shallow';

const PROMPT_FIELDS = [
    {
        containerClassName: 'min-h-0 overflow-hidden',
        editorClassName: 'min-h-[120px]',
        path: 'systemPrompt',
        placeholder:
            "System instructions that define the agent's behavior, role, and constraints. Use '$' to insert data pills.",
        title: 'Instructions to follow',
    },
    {
        // A single row like Attachments below: the user input is usually one data pill or a short line, and
        // the editor grows as content is added.
        editorClassName: 'p-2',
        path: 'userPrompt',
        placeholder: "The message sent to the agent on each execution. Use '$' to insert data pills.",
        title: 'User input',
    },
    {
        editorClassName: 'p-2',
        path: 'attachments',
        placeholder: "File data pill references to attach to the message. Use '$' to insert data pills.",
        title: 'Attachments',
    },
];

export function AiAgentConfigurationPanel() {
    const {componentDefinitions, dataPills, taskDispatcherDefinitions, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            dataPills: state.dataPills,
            taskDispatcherDefinitions: state.taskDispatcherDefinitions,
            workflow: state.workflow,
        }))
    );

    return (
        <div className="flex flex-col gap-4">
            <h2 className="font-medium">Configuration</h2>

            <AiAgentModelSelectField />

            <AiAgentStreamResponseField />

            {PROMPT_FIELDS.map((field) => (
                <AiAgentPromptField
                    componentDefinitions={componentDefinitions}
                    containerClassName={field.containerClassName}
                    dataPills={dataPills}
                    editorClassName={field.editorClassName}
                    key={field.path}
                    path={field.path}
                    placeholder={field.placeholder}
                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                    title={field.title}
                    workflow={workflow}
                />
            ))}

            <AiAgentTools />

            <AiAgentSkills />
        </div>
    );
}
