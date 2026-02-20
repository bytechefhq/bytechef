import AiAgentModelSelectField from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentModelSelectField';
import AiAgentPromptField from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentPromptField';
import AiAgentTools from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-configuration-panel/components/AiAgentTools';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useShallow} from 'zustand/shallow';

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

            <AiAgentPromptField
                componentDefinitions={componentDefinitions}
                containerClassName="min-h-0 overflow-hidden"
                dataPills={dataPills}
                editorClassName="min-h-[200px]"
                path="systemPrompt"
                placeholder="System instructions that define the agent's behavior, role, and constraints. Use '$' to insert data pills."
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                title="Instructions to follow:"
                workflow={workflow}
            />

            <AiAgentPromptField
                componentDefinitions={componentDefinitions}
                dataPills={dataPills}
                editorClassName="min-h-[100px]"
                path="userPrompt"
                placeholder="The message sent to the agent on each execution. Use '$' to insert data pills."
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                title="User input:"
                workflow={workflow}
            />

            <AiAgentPromptField
                componentDefinitions={componentDefinitions}
                dataPills={dataPills}
                editorClassName="p-2"
                path="attachments"
                placeholder="File data pill references to attach to the message. Use '$' to insert data pills."
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                title="Attachments:"
                workflow={workflow}
            />

            <AiAgentTools />
        </div>
    );
}
