import {
    WireNodeConnectionRequestI,
    useCopyTemplateMutation,
    useDeleteAutomationMutation,
    usePublishAutomationMutation,
    useSetAutomationEnabledMutation,
    useUpdateAutomationInputsMutation,
    useWireNodeConnectionMutation,
} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {useFetchWorkflow} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {
    ActivationActionType,
    ActivationStateI,
    activationReducer,
    initialActivationState,
} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {
    AutomationWorkflowProjectWorkflowTemplate,
    MissingConnectionError,
    ResponseError,
} from '@/ee/shared/middleware/embedded/public';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {Dispatch, useCallback, useMemo, useReducer, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.';
const UNREADABLE_DEFINITION_MESSAGE = 'The copied automation could not be read.';
const WIRING_ERROR_MESSAGE = 'Your accounts could not be connected to this automation. Please try again.';

interface WorkflowNodeI {
    connections?: unknown;
    name?: string;
    type?: string;
}

interface WorkflowDefinitionI {
    tasks?: WorkflowNodeI[];
    triggers?: WorkflowNodeI[];
}

interface WorkflowNodeConnectionI {
    componentName: string;
    workflowConnectionKey: string;
}

interface ActivationFlowI {
    activate: () => Promise<void>;
    busy: boolean;
    dispatch: Dispatch<ActivationActionType>;
    editWorkflow: () => Promise<void>;
    openInBuilder: () => void;
    state: ActivationStateI;
}

const toErrorMessage = (error: unknown): string => {
    if (error instanceof ResponseError) {
        return GENERIC_ERROR_MESSAGE;
    }

    return error instanceof Error && error.message ? error.message : GENERIC_ERROR_MESSAGE;
};

const readMissingConnectionComponentName = async (error: unknown): Promise<string | undefined> => {
    if (!(error instanceof ResponseError) || error.response.status !== 409) {
        return undefined;
    }

    try {
        const body = (await error.response.json()) as MissingConnectionError;

        return body?.missingConnectionComponentName;
    } catch {
        return undefined;
    }
};

const readWorkflowNodeConnections = (node: WorkflowNodeI, nodeComponentName: string): WorkflowNodeConnectionI[] => {
    const {connections} = node;

    if (Array.isArray(connections) && connections.length > 0) {
        return connections.map((connection) => {
            const componentName = (connection?.componentName as string) || nodeComponentName;

            return {componentName, workflowConnectionKey: (connection?.key as string) || componentName};
        });
    }

    if (connections && typeof connections === 'object') {
        const entries = Object.entries(connections as Record<string, {componentName?: string}>);

        if (entries.length > 0) {
            return entries.map(([workflowConnectionKey, connection]) => ({
                componentName: connection?.componentName || nodeComponentName,
                workflowConnectionKey,
            }));
        }
    }

    return [{componentName: nodeComponentName, workflowConnectionKey: nodeComponentName}];
};

const buildWiringRequests = (
    definition: WorkflowDefinitionI,
    selections: Record<string, number | undefined>,
    workflowUuid: string
): WireNodeConnectionRequestI[] => {
    const requests: WireNodeConnectionRequestI[] = [];

    for (const node of [...(definition.triggers || []), ...(definition.tasks || [])]) {
        const nodeComponentName = String(node.type || '').split('/')[0];

        for (const nodeConnection of readWorkflowNodeConnections(node, nodeComponentName)) {
            const connectionId = selections[nodeConnection.componentName];

            if (connectionId == null || !node.name) {
                continue;
            }

            requests.push({
                connectionId,
                workflowConnectionKey: nodeConnection.workflowConnectionKey,
                workflowNodeName: node.name,
                workflowUuid,
            });
        }
    }

    return requests;
};

export const useRequiredComponents = (template: AutomationWorkflowProjectWorkflowTemplate) => {
    const {
        data: connectionComponentDefinitions,
        error,
        isLoading,
        refetch,
    } = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const requiredComponents = useMemo(() => {
        const connectionComponentNames = new Set(
            (connectionComponentDefinitions || []).map((componentDefinition) => componentDefinition.name)
        );

        const componentNames: string[] = [];

        for (const component of template.components || []) {
            if (
                component.name &&
                connectionComponentNames.has(component.name) &&
                !componentNames.includes(component.name)
            ) {
                componentNames.push(component.name);
            }
        }

        return componentNames;
    }, [connectionComponentDefinitions, template.components]);

    const isError = !!error && connectionComponentDefinitions === undefined;

    return {isError, isLoading, refetch, requiredComponents};
};

export const useActivationFlow = (
    template: AutomationWorkflowProjectWorkflowTemplate,
    requiredComponents: string[]
): ActivationFlowI => {
    const [pending, setPending] = useState(false);

    const copiedWorkflowUuidRef = useRef<string>(undefined);

    const templateInputs = useMemo(
        () =>
            (template.inputs ?? [])
                .filter((input): input is typeof input & {name: string} => !!input.name)
                .map(({label, name, required, type}) => ({label, name, required, type})),
        [template.inputs]
    );

    const [state, dispatch] = useReducer(activationReducer, initialActivationState(requiredComponents, templateInputs));

    const navigate = useNavigate();

    const fetchWorkflow = useFetchWorkflow();

    const {mutateAsync: copyTemplate} = useCopyTemplateMutation();
    const {mutateAsync: deleteAutomation} = useDeleteAutomationMutation();
    const {mutateAsync: publishAutomation} = usePublishAutomationMutation();
    const {mutateAsync: updateAutomationInputs} = useUpdateAutomationInputsMutation();
    const {mutateAsync: setAutomationEnabled} = useSetAutomationEnabledMutation();
    const {mutateAsync: wireNodeConnection} = useWireNodeConnectionMutation();

    const templateUuid = template.id!;

    const wireCopiedWorkflow = useCallback(
        async (workflowUuid: string) => {
            let definitionJson: string | undefined;

            try {
                const workflow = await fetchWorkflow(workflowUuid);

                definitionJson = workflow.definition;
            } catch {
                throw new Error(WIRING_ERROR_MESSAGE);
            }

            let definition: WorkflowDefinitionI;

            try {
                definition = JSON.parse(definitionJson || '') as WorkflowDefinitionI;
            } catch {
                throw new Error(UNREADABLE_DEFINITION_MESSAGE);
            }

            try {
                await buildWiringRequests(definition, state.selections, workflowUuid).reduce<Promise<void>>(
                    (previousWiring, request) => previousWiring.then(() => wireNodeConnection(request)),
                    Promise.resolve()
                );
            } catch {
                throw new Error(WIRING_ERROR_MESSAGE);
            }
        },
        [fetchWorkflow, state.selections, wireNodeConnection]
    );

    const activate = useCallback(async () => {
        setPending(true);

        let createdWorkflowUuid: string | undefined;

        const undoPartialActivation = async () => {
            try {
                if (createdWorkflowUuid) {
                    await deleteAutomation(createdWorkflowUuid);

                    copiedWorkflowUuidRef.current = undefined;
                }
            } catch {
                return;
            }
        };

        try {
            let workflowUuid: string;

            if (copiedWorkflowUuidRef.current) {
                workflowUuid = copiedWorkflowUuidRef.current;
            } else {
                workflowUuid = await copyTemplate(templateUuid);

                copiedWorkflowUuidRef.current = workflowUuid;
                createdWorkflowUuid = workflowUuid;
            }

            await wireCopiedWorkflow(workflowUuid);

            await publishAutomation(workflowUuid);

            if (state.inputs.length > 0) {
                await updateAutomationInputs({inputs: state.inputValues, workflowUuid});
            }

            await setAutomationEnabled({enabled: true, workflowUuid});

            dispatch({type: 'ACTIVATED', workflowUuid});
        } catch (error) {
            const missingConnectionComponentName = await readMissingConnectionComponentName(error);

            await undoPartialActivation();

            if (missingConnectionComponentName) {
                dispatch({componentName: missingConnectionComponentName, type: 'MISSING_CONNECTION'});
            } else {
                dispatch({error: toErrorMessage(error), type: 'FAILED'});
            }
        } finally {
            setPending(false);
        }
    }, [
        copyTemplate,
        deleteAutomation,
        publishAutomation,
        setAutomationEnabled,
        state.inputValues,
        state.inputs.length,
        updateAutomationInputs,
        templateUuid,
        wireCopiedWorkflow,
    ]);

    const openInBuilder = useCallback(() => {
        void navigate(`/embedded/hub/builder/${state.workflowUuid}`);
    }, [navigate, state.workflowUuid]);

    const editWorkflow = useCallback(async () => {
        const existingWorkflowUuid = state.workflowUuid || copiedWorkflowUuidRef.current;

        if (existingWorkflowUuid) {
            void navigate(`/embedded/hub/builder/${existingWorkflowUuid}`);

            return;
        }

        setPending(true);

        try {
            const workflowUuid = await copyTemplate(templateUuid);

            copiedWorkflowUuidRef.current = workflowUuid;

            dispatch({type: 'COPIED', workflowUuid});

            void navigate(`/embedded/hub/builder/${workflowUuid}`);
        } catch (error) {
            dispatch({error: toErrorMessage(error), type: 'FAILED'});
        } finally {
            setPending(false);
        }
    }, [copyTemplate, navigate, state.workflowUuid, templateUuid]);

    return {
        activate,
        busy: pending,
        dispatch,
        editWorkflow,
        openInBuilder,
        state,
    };
};
