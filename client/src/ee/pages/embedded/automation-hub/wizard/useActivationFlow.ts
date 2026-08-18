import {
    WireNodeConnectionRequestI,
    useCopyTemplateMutation,
    useDeprovisionReferenceMutation,
    useProvisionReferenceMutation,
    usePublishAutomationMutation,
    useSetAutomationEnabledMutation,
    useWireNodeConnectionMutation,
} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {useGetWorkflowQuery} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {
    ActivationActionType,
    ActivationStateI,
    activationReducer,
    initialActivationState,
} from '@/ee/pages/embedded/automation-hub/wizard/activationReducer';
import {
    AutomationWorkflowProjectKindEnum,
    AutomationWorkflowProjectWorkflowTemplate,
    MissingConnectionError,
    ResponseError,
} from '@/ee/shared/middleware/embedded/public';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {Dispatch, useCallback, useEffect, useMemo, useReducer, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.';
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

export interface ActivationFlowI {
    activate: () => Promise<void>;
    busy: boolean;
    configure: () => Promise<void>;
    dispatch: Dispatch<ActivationActionType>;
    openInBuilder: () => void;
    retryWiring: () => Promise<void>;
    state: ActivationStateI;
    wiringComplete: boolean;
    wiringFailed: boolean;
}

/**
 * A `ResponseError` message is the SDK's own boilerplate ("Response returned an error code"), never
 * anything the connected user should read, so only a genuine `Error` message is surfaced as-is.
 */
const toErrorMessage = (error: unknown): string => {
    if (error instanceof ResponseError) {
        return GENERIC_ERROR_MESSAGE;
    }

    return error instanceof Error && error.message ? error.message : GENERIC_ERROR_MESSAGE;
};

/**
 * Both the provision and the enable endpoints report a connection they could not auto-wire as a
 * `MissingConnectionError` body on a 409; anything else is a plain failure.
 */
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

/**
 * The connection keys a copied workflow node exposes, paired with the component each one belongs
 * to. A plain component task declares nothing about connections in the definition — the platform
 * derives a single connection whose key IS the component name
 * (`DefaultComponentConnectionFactory` via `ComponentConnection.of`) — which is why the fallback,
 * not the two branches above it, is what fires for every node of a visual template today.
 *
 * The list and map branches are defensive only: a task that DOES declare connections carries them
 * under `extensions.connections`, which this reader does not descend into, so neither branch
 * matches a definition the server writes. They are kept because they cost nothing and make a
 * hand-authored or future definition shape degrade into correct keys rather than into the
 * component-name fallback.
 */
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

/**
 * The template components the wizard has to ask for an account: the ones whose component
 * definition actually declares a connection. `connectionDefinitions: true` makes the shared
 * component-definition listing return exactly those, and the hub already loads it elsewhere, so
 * this costs no extra request. Components without a connection are skipped; when none remain the
 * reducer starts the wizard on the configure step.
 *
 * The query's failure is surfaced as `isError` rather than collapsing the list to empty on any
 * error, because an empty list is indistinguishable from "this template needs no accounts": the
 * wizard would skip the connect step, wire nothing, and activate a copy whose connections are
 * missing — a failure the user only meets at run time. The caller must refuse to start the flow
 * on it.
 *
 * `isError` is deliberately NOT just `!!error`: TanStack Query retains `data` from the last
 * successful fetch across a failed background refetch (this query has a 5-minute `staleTime` and
 * the default `refetchOnWindowFocus`, and `HubConnectionDialog` mounts a second observer of the
 * same query key), so `error` alone can be true while `connectionComponentDefinitions` is still
 * the last good list. Gating on plain `error` would unmount an already-open wizard mid-flight —
 * `isError` is true only when the lookup has NEVER produced data, which is the only case the
 * caller must actually refuse to start on.
 */
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

/**
 * Owns the activation reducer plus every mutation the three wizard steps drive, and branches the
 * two catalog kinds:
 *
 * - `COPY` copies the template into the connected user's own project, wires the selected
 *   connections onto the copy's nodes, and must PUBLISH the copy before enabling it —
 *   `enableProjectWorkflow` refuses a copy that is not in the active deployment.
 * - `REFERENCE` provisions a reference against the shared catalog workflow, which auto-wires by
 *   component match, and enables it directly.
 *
 * Wiring is awaited rather than fired and forgotten: `publishProjectWorkflow` snapshots the
 * workflow's connections into the deployment, so publishing while a wiring PUT is still in flight —
 * or after one failed — produces an enabled deployment with a partial connection list that only
 * fails at run time. `wiringComplete` therefore gates the configure step's Next button.
 */
export const useActivationFlow = (
    template: AutomationWorkflowProjectWorkflowTemplate,
    kind: AutomationWorkflowProjectKindEnum,
    requiredComponents: string[]
): ActivationFlowI => {
    const [pending, setPending] = useState(false);
    const [wiredKey, setWiredKey] = useState<string>();
    const [wiringRejected, setWiringRejected] = useState(false);
    const [wiringPending, setWiringPending] = useState(false);

    const copiedWorkflowUuidRef = useRef<string>(undefined);
    const referenceRowExistsRef = useRef(false);
    const reportedDefinitionErrorRef = useRef<string>(undefined);
    const unreadableDefinitionRef = useRef<string>(undefined);
    const wiringRequestsRef = useRef<WireNodeConnectionRequestI[]>([]);
    const wiringRunRef = useRef(0);
    const wiringStartedKeyRef = useRef<string>(undefined);

    const [state, dispatch] = useReducer(activationReducer, initialActivationState(kind, requiredComponents));

    const navigate = useNavigate();

    const {mutateAsync: copyTemplate} = useCopyTemplateMutation();
    const {mutateAsync: deprovisionReference} = useDeprovisionReferenceMutation();
    const {mutateAsync: provisionReference} = useProvisionReferenceMutation();
    const {mutateAsync: publishAutomation} = usePublishAutomationMutation();
    const {mutateAsync: setAutomationEnabled} = useSetAutomationEnabledMutation();
    const {mutateAsync: wireNodeConnection} = useWireNodeConnectionMutation();

    const {
        data: copiedWorkflow,
        error: copiedWorkflowError,
        refetch: refetchCopiedWorkflow,
    } = useGetWorkflowQuery(kind === 'COPY' ? state.workflowUuid : undefined);

    const templateUuid = template.id!;

    const wiringKey = useMemo(
        () => (state.workflowUuid ? `${state.workflowUuid}:${JSON.stringify(state.selections)}` : undefined),
        [state.selections, state.workflowUuid]
    );

    // A COPY is only ready to publish once its connections are actually on the workflow. A
    // REFERENCE is auto-wired server-side and has nothing to wait for.
    const wiringComplete = kind !== 'COPY' || !state.workflowUuid || wiredKey === wiringKey;

    // A definition that never arrived is as blocking as a rejected PUT, and just as retryable — the
    // configure step would otherwise sit on "Connecting your accounts…" forever with no way out.
    const wiringFailed = wiringRejected || (kind === 'COPY' && !!state.workflowUuid && !!copiedWorkflowError);

    const runWiring = useCallback(
        async (key: string, requests: WireNodeConnectionRequestI[]) => {
            // Nothing cancels an in-flight run, so a superseded one must not write any state: the
            // user can change a selection while the previous key's PUTs are still going, and an
            // older run finishing last would otherwise strand `wiredKey` on the stale key —
            // blocking Next with no failure to explain it and no retry on screen.
            const runId = wiringRunRef.current + 1;

            wiringRunRef.current = runId;

            const isCurrentRun = () => wiringRunRef.current === runId;

            setWiringRejected(false);

            if (requests.length === 0) {
                setWiredKey(key);

                return;
            }

            setWiringPending(true);

            try {
                // Sequential rather than parallel: these all PUT onto the same workflow, and a
                // deterministic order makes a partial failure easy to reason about.
                for (const request of requests) {
                    await wireNodeConnection(request);

                    if (!isCurrentRun()) {
                        return;
                    }
                }

                setWiredKey(key);
            } catch {
                if (isCurrentRun()) {
                    setWiringRejected(true);

                    dispatch({error: WIRING_ERROR_MESSAGE, type: 'FAILED'});
                }
            } finally {
                if (isCurrentRun()) {
                    setWiringPending(false);
                }
            }
        },
        [wireNodeConnection]
    );

    const retryWiring = useCallback(async () => {
        const startedKey = wiringStartedKeyRef.current;

        // Wiring never got as far as issuing a PUT — the copied workflow's definition is what
        // failed to load. Reload it and let the wiring effect pick up from there.
        if (!startedKey || startedKey !== wiringKey) {
            setWiringRejected(false);

            reportedDefinitionErrorRef.current = undefined;
            unreadableDefinitionRef.current = undefined;
            wiringStartedKeyRef.current = undefined;

            await refetchCopiedWorkflow();

            return;
        }

        await runWiring(startedKey, wiringRequestsRef.current);
    }, [refetchCopiedWorkflow, runWiring, wiringKey]);

    const configure = useCallback(async () => {
        setPending(true);

        try {
            if (kind === 'COPY') {
                // Reuse the copy this wizard already made. `configure` runs again whenever the
                // reducer clears `workflowUuid` (a missing-connection 409 does exactly that), and
                // copying a second time would leave an orphaned automation behind.
                const workflowUuid = copiedWorkflowUuidRef.current || (await copyTemplate(templateUuid));

                copiedWorkflowUuidRef.current = workflowUuid;

                dispatch({type: 'COPIED', workflowUuid});
            } else {
                // Only two outcomes leave a reference row behind: a successful provision, and a
                // missing-connection 409 — `getOrCreateReference` is
                // `@Transactional(noRollbackFor = MissingConnectionException.class)` precisely so
                // that case keeps its disabled row. Every OTHER failure rolls the row back, and
                // de-provisioning then throws WORKFLOW_NOT_FOUND, which would dead-end every
                // subsequent retry. So the flag is armed by those two outcomes only — never by the
                // mere fact that an attempt was made, and never read off
                // `state.highlightedComponent`, which the user's re-selection clears.
                if (referenceRowExistsRef.current) {
                    await deprovisionReference(templateUuid);

                    referenceRowExistsRef.current = false;
                }

                await provisionReference(templateUuid);

                referenceRowExistsRef.current = true;

                dispatch({type: 'PROVISIONED', workflowUuid: templateUuid});
            }
        } catch (error) {
            const missingConnectionComponentName = await readMissingConnectionComponentName(error);

            if (missingConnectionComponentName) {
                referenceRowExistsRef.current = kind === 'REFERENCE';

                dispatch({componentName: missingConnectionComponentName, type: 'MISSING_CONNECTION'});
            } else {
                dispatch({error: toErrorMessage(error), type: 'FAILED'});
            }
        } finally {
            setPending(false);
        }
    }, [copyTemplate, deprovisionReference, kind, provisionReference, templateUuid]);

    const activate = useCallback(async () => {
        const workflowUuid = state.workflowUuid;

        if (!workflowUuid) {
            return;
        }

        setPending(true);

        try {
            if (kind === 'COPY') {
                await publishAutomation(workflowUuid);
            }

            await setAutomationEnabled({enabled: true, workflowUuid});

            dispatch({type: 'ACTIVATED'});
        } catch (error) {
            // `doEnableProjectWorkflow` reports a connection it could not resolve with the same
            // 409 body the provision path uses, so an enable-time miss drives the same highlight
            // loop instead of an opaque failure message.
            const missingConnectionComponentName = await readMissingConnectionComponentName(error);

            if (missingConnectionComponentName) {
                dispatch({componentName: missingConnectionComponentName, type: 'MISSING_CONNECTION'});
            } else {
                dispatch({error: toErrorMessage(error), type: 'FAILED'});
            }
        } finally {
            setPending(false);
        }
    }, [kind, publishAutomation, setAutomationEnabled, state.workflowUuid]);

    const openInBuilder = useCallback(() => {
        navigate(`/embedded/hub/builder/${state.workflowUuid}`);
    }, [navigate, state.workflowUuid]);

    useEffect(() => {
        const workflowUuid = state.workflowUuid;

        if (kind !== 'COPY' || !workflowUuid || !wiringKey || !copiedWorkflow?.definition) {
            return;
        }

        // Keyed on the selections as well as the copy, so going Back and picking a different
        // account re-wires the copy instead of silently keeping the first choice; an unchanged
        // selection never issues the same PUT twice. `state.step` is a dependency so that
        // re-entering the configure step after the reset below genuinely re-runs the wiring, even
        // when the user re-picked the very same connection.
        if (wiringStartedKeyRef.current === wiringKey) {
            return;
        }

        // A definition that already failed to parse is not retried on sight: `copiedWorkflow` is a
        // fresh object on every query render, so without this the effect would re-enter — and
        // re-dispatch — forever. `retryWiring` clears the ref, which is what makes Try again reload.
        if (unreadableDefinitionRef.current === copiedWorkflow.definition) {
            return;
        }

        let definition: WorkflowDefinitionI;

        try {
            definition = JSON.parse(copiedWorkflow.definition) as WorkflowDefinitionI;
        } catch {
            unreadableDefinitionRef.current = copiedWorkflow.definition;

            // `wiringRejected` is what makes `wiringFailed` true, and without it the configure step
            // rendered "Connecting your accounts…" forever underneath this very banner. The wiring
            // key stays UNCLAIMED so `retryWiring` takes its reload branch — there are no requests
            // to re-issue, the definition itself is what has to come back.
            setWiringRejected(true);

            dispatch({error: 'The copied automation could not be read.', type: 'FAILED'});

            return;
        }

        wiringStartedKeyRef.current = wiringKey;

        wiringRequestsRef.current = buildWiringRequests(definition, state.selections, workflowUuid);

        void runWiring(wiringKey, wiringRequestsRef.current);
    }, [copiedWorkflow, kind, runWiring, state.selections, state.step, state.workflowUuid, wiringKey]);

    // Declared AFTER the wiring effect on purpose: on the render where the step becomes `connect`
    // the wiring effect runs first and still sees its own key, so clearing the key here cannot
    // kick off a wiring run while the user is still choosing accounts.
    useEffect(() => {
        if (state.step !== 'connect' || !wiringFailed) {
            return;
        }

        // Back from a failed wiring clears `state.error` and unmounts the configure step's Try
        // again button, so without this the wizard would sit on the connect step with nothing to
        // explain itself and nothing able to re-run the wiring — an identical re-selection
        // produces an identical key and short-circuits the effect above.
        wiringStartedKeyRef.current = undefined;

        // Re-armed alongside it: BACK clears `state.error`, so leaving these set would bring the
        // user back to a configure step carrying a Try again button and nothing that says why.
        reportedDefinitionErrorRef.current = undefined;
        unreadableDefinitionRef.current = undefined;

        setWiringRejected(false);
    }, [state.step, wiringFailed]);

    useEffect(() => {
        // Scoped to the configure step: BACK clears `state.error` on purpose, and re-reporting on
        // the connect step would put a banner next to accounts the user is being asked to re-pick.
        if (kind !== 'COPY' || state.step !== 'configure' || !state.workflowUuid || !copiedWorkflowError) {
            return;
        }

        // Reported once per copy: a failed query re-renders with a fresh Error instance, and
        // dispatching on every one of those would spin the reducer forever. `wiringFailed` stays
        // derived from the live error, so Try again remains on screen either way, and the retry
        // clears this ref so a second failure is reported again.
        if (reportedDefinitionErrorRef.current === state.workflowUuid) {
            return;
        }

        reportedDefinitionErrorRef.current = state.workflowUuid;

        dispatch({error: WIRING_ERROR_MESSAGE, type: 'FAILED'});
        // `state.step` is a dependency so that RETURNING to the configure step re-reports a still
        // failing load: the reset effect above cleared the ref, and nothing else in this list
        // changes on a BACK/NEXT round trip.
    }, [copiedWorkflowError, kind, state.step, state.workflowUuid]);

    return {
        activate,
        busy: pending || wiringPending,
        configure,
        dispatch,
        openInBuilder,
        retryWiring,
        state,
        wiringComplete,
        wiringFailed,
    };
};
