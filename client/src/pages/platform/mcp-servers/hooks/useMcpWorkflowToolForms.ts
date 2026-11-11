import useMcpWorkflowToolSeeding, {
    LoadWorkflowToolPropertiesType,
} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolSeeding';
import buildToolParametersPayload from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import pickChangedValues from '@/pages/platform/mcp-servers/utils/pickChangedValues';
import {toStoredWorkflowParameters} from '@/pages/platform/mcp-servers/utils/toolParameterConversion';
import {WorkflowToolParametersInput} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {createFormControl} from 'react-hook-form';

export type SavedToolParametersByWorkflowIdType = Map<string, Record<string, unknown> | null | undefined>;

type ToolFormControlType = ReturnType<typeof createFormControl<Record<string, unknown>>>;

interface McpWorkflowToolFormStateI {
    formControl: ToolFormControlType;
    initialValues?: Record<string, unknown>;
}

interface UseMcpWorkflowToolFormsParamsI {
    loadWorkflowProperties: LoadWorkflowToolPropertiesType;
    savedParametersByWorkflowId: SavedToolParametersByWorkflowIdType;
    seedKey: string;
    selectedWorkflowIds: string[];
}

export default function useMcpWorkflowToolForms({
    loadWorkflowProperties,
    savedParametersByWorkflowId,
    seedKey,
    selectedWorkflowIds,
}: UseMcpWorkflowToolFormsParamsI) {
    const [isAwaitingSeeding, setIsAwaitingSeeding] = useState(false);

    const toolFormsByWorkflowIdRef = useRef<Map<string, McpWorkflowToolFormStateI>>(new Map());

    const newWorkflowIds = useMemo(
        () => selectedWorkflowIds.filter((workflowId) => !savedParametersByWorkflowId.has(workflowId)),
        [savedParametersByWorkflowId, selectedWorkflowIds]
    );

    const {aiValuesByWorkflowId, waitForSeeding} = useMcpWorkflowToolSeeding({
        loadWorkflowProperties,
        newWorkflowIds,
        seedKey,
    });

    const selectedWorkflowIdsKey = selectedWorkflowIds.join(',');

    const isNewWorkflow = (workflowId: string) => !savedParametersByWorkflowId.has(workflowId);

    const getToolForm = useCallback((workflowId: string): McpWorkflowToolFormStateI => {
        const toolFormsByWorkflowId = toolFormsByWorkflowIdRef.current;

        let toolForm = toolFormsByWorkflowId.get(workflowId);

        if (!toolForm) {
            toolForm = {formControl: createFormControl<Record<string, unknown>>({defaultValues: {}})};

            toolFormsByWorkflowId.set(workflowId, toolForm);
        }

        return toolForm;
    }, []);

    const setToolFormInitialValues = useCallback(
        (workflowId: string, initialValues: Record<string, unknown>) => {
            getToolForm(workflowId).initialValues = initialValues;
        },
        [getToolForm]
    );

    const buildToolParameters = async (workflowIds: string[]): Promise<WorkflowToolParametersInput[]> => {
        setIsAwaitingSeeding(true);

        let seededValuesByWorkflowId: Record<string, Record<string, string>>;

        try {
            seededValuesByWorkflowId = await waitForSeeding();
        } finally {
            setIsAwaitingSeeding(false);
        }

        return workflowIds.flatMap((workflowId) => {
            const isNew = isNewWorkflow(workflowId);
            const toolForm = toolFormsByWorkflowIdRef.current.get(workflowId);

            const changedValues = toolForm?.initialValues
                ? pickChangedValues(
                      toolForm.formControl.getValues(),
                      toolForm.initialValues,
                      (path) => toolForm.formControl.getFieldState(path).isDirty
                  )
                : {};

            if (!isNew && Object.keys(changedValues).length === 0) {
                return [];
            }

            return [
                {
                    parameters: toStoredWorkflowParameters(
                        buildToolParametersPayload({
                            aiValues: isNew ? (seededValuesByWorkflowId[workflowId] ?? {}) : {},
                            dirtyValues: changedValues,
                            savedParameters: isNew ? undefined : savedParametersByWorkflowId.get(workflowId),
                        })
                    ),
                    workflowId,
                },
            ];
        });
    };

    useEffect(() => {
        const currentWorkflowIds = selectedWorkflowIdsKey ? selectedWorkflowIdsKey.split(',') : [];
        const toolFormsByWorkflowId = toolFormsByWorkflowIdRef.current;

        for (const workflowId of Array.from(toolFormsByWorkflowId.keys())) {
            if (!currentWorkflowIds.includes(workflowId)) {
                toolFormsByWorkflowId.delete(workflowId);
            }
        }
    }, [selectedWorkflowIdsKey]);

    return {
        aiValuesByWorkflowId,
        buildToolParameters,
        getToolForm,
        isAwaitingSeeding,
        isNewWorkflow,
        savedParametersByWorkflowId,
        setToolFormInitialValues,
    };
}

export type McpWorkflowToolFormsType = ReturnType<typeof useMcpWorkflowToolForms>;
