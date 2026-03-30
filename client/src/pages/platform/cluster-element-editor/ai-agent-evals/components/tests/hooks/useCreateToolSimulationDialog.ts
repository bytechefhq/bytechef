import {useState} from 'react';

interface ToolSimulationEditDataI {
    id: string;
    responsePrompt: string;
    simulationModel?: string | null;
    toolName: string;
}

interface UseCreateToolSimulationDialogProps {
    editData?: ToolSimulationEditDataI;
    onClose: () => void;
    onCreate: (toolName: string, responsePrompt: string, simulationModel?: string) => Promise<void>;
    onUpdate?: (id: string, toolName?: string, responsePrompt?: string, simulationModel?: string) => Promise<void>;
}

export default function useCreateToolSimulationDialog({
    editData,
    onClose,
    onCreate,
    onUpdate,
}: UseCreateToolSimulationDialogProps) {
    const [responsePrompt, setResponsePrompt] = useState(editData?.responsePrompt ?? '');
    const [simulationModel, setSimulationModel] = useState(editData?.simulationModel ?? '');
    const [submitting, setSubmitting] = useState(false);
    const [toolName, setToolName] = useState(editData?.toolName ?? '');

    const isEditing = !!editData;

    const handleSubmit = async () => {
        if (!toolName.trim() || !responsePrompt.trim()) {
            return;
        }

        setSubmitting(true);

        let succeeded = false;

        try {
            if (isEditing && editData && onUpdate) {
                await onUpdate(
                    editData.id,
                    toolName.trim(),
                    responsePrompt.trim(),
                    simulationModel.trim() || undefined
                );
            } else {
                await onCreate(toolName.trim(), responsePrompt.trim(), simulationModel.trim() || undefined);
            }

            succeeded = true;
        } catch (error: unknown) {
            console.error('Tool simulation submission failed:', error);
        } finally {
            setSubmitting(false);
        }

        if (succeeded) {
            onClose();
        }
    };

    return {
        handleSubmit,
        isEditing,
        responsePrompt,
        setResponsePrompt,
        setSimulationModel,
        setToolName,
        simulationModel,
        submitting,
        toolName,
    };
}

export type {ToolSimulationEditDataI};
