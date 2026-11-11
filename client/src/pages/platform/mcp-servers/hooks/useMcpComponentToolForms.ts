import {McpToolFormRegistrationI} from '@/pages/platform/mcp-servers/components/McpComponentDialogToolForm';
import useMcpToolSeeding from '@/pages/platform/mcp-servers/hooks/useMcpToolSeeding';
import buildToolParametersPayload from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import pickChangedValues from '@/pages/platform/mcp-servers/utils/pickChangedValues';
import {useCallback, useMemo, useRef, useState} from 'react';

interface ExistingToolI {
    name: string;
    parameters?: unknown;
}

interface SelectedToolI {
    name: string;
}

interface UseMcpComponentToolFormsParamsI {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    existingTools?: Array<ExistingToolI | null> | null;
    selectedTools: SelectedToolI[];
}

export type SavedToolParametersByNameType = Map<string, Record<string, unknown> | null | undefined>;

interface McpComponentToolPayloadI {
    name: string;
    parameters: Record<string, unknown>;
}

export function pickToolFormChanges({form, initialValues}: McpToolFormRegistrationI): Record<string, unknown> {
    return pickChangedValues(form.getValues(), initialValues, (path) => form.getFieldState(path).isDirty);
}

export default function useMcpComponentToolForms({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    existingTools,
    selectedTools,
}: UseMcpComponentToolFormsParamsI) {
    const [isAwaitingSeeding, setIsAwaitingSeeding] = useState(false);

    const toolFormsRef = useRef<Map<string, McpToolFormRegistrationI>>(new Map());

    const savedParametersByToolName: SavedToolParametersByNameType = useMemo(
        () =>
            new Map(
                (existingTools ?? [])
                    .filter((existingTool): existingTool is ExistingToolI => existingTool !== null)
                    .map((existingTool) => [
                        existingTool.name,
                        existingTool.parameters as Record<string, unknown> | null | undefined,
                    ])
            ),
        [existingTools]
    );

    const newToolNames = useMemo(
        () =>
            selectedTools
                .map((selectedTool) => selectedTool.name)
                .filter((toolName) => !savedParametersByToolName.has(toolName)),
        [savedParametersByToolName, selectedTools]
    );

    const {aiValuesByTool, waitForSeeding} = useMcpToolSeeding({
        componentName,
        componentVersion,
        connectionId,
        connectionRequired,
        newToolNames,
    });

    const registerToolForm = useCallback((toolName: string, registration: McpToolFormRegistrationI | null) => {
        if (registration) {
            toolFormsRef.current.set(toolName, registration);
        } else {
            toolFormsRef.current.delete(toolName);
        }
    }, []);

    const buildToolsPayload = async (tools: SelectedToolI[]): Promise<McpComponentToolPayloadI[]> => {
        setIsAwaitingSeeding(true);

        let seededValuesByTool: Record<string, Record<string, string>>;

        try {
            seededValuesByTool = await waitForSeeding();
        } finally {
            setIsAwaitingSeeding(false);
        }

        return tools.map(({name: toolName}) => {
            const isNew = !savedParametersByToolName.has(toolName);
            const toolForm = toolFormsRef.current.get(toolName);

            return {
                name: toolName,
                parameters: buildToolParametersPayload({
                    aiValues: isNew ? (seededValuesByTool[toolName] ?? {}) : {},
                    dirtyValues: toolForm ? pickToolFormChanges(toolForm) : {},
                    savedParameters: isNew ? undefined : savedParametersByToolName.get(toolName),
                }),
            };
        });
    };

    return {
        aiValuesByTool,
        buildToolsPayload,
        isAwaitingSeeding,
        registerToolForm,
        savedParametersByToolName,
    };
}
