import Button from '@/components/Button/Button';
import {HubBuilderContext} from '@/ee/pages/embedded/automation-hub/hubBuilderContext';
import {AutomationHubKeys, useGetWorkflowQuery} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import WorkflowBuilder from '@/ee/pages/embedded/workflow-builder/WorkflowBuilder';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

/**
 * Opens the workflow builder inside the hub's own iframe as an internal route (spec D3), so a
 * vendor embeds one component instead of wiring the builder separately. Deliberately a sibling of
 * `hub` in the route tree rather than nested under `AutomationHubLayout`/`RequireTab` — the
 * builder owns the whole surface and has no tab strip of its own.
 *
 * The hub already completed the EMBED_READY/EMBED_INIT handshake before this route was ever
 * reachable, so its three vendor-supplied settings are forwarded to the builder via
 * `HubBuilderContext` instead of the builder repeating the handshake for itself.
 */
const HubBuilderView = () => {
    const {connectionDialogAllowed, includeComponents, sharedConnectionIds} = useAutomationHubStore(
        useShallow((state) => ({
            connectionDialogAllowed: state.connectionDialogAllowed,
            includeComponents: state.includeComponents,
            sharedConnectionIds: state.sharedConnectionIds,
        }))
    );

    const {workflowUuid} = useParams();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const {data: automation} = useGetWorkflowQuery(workflowUuid);

    // `useWorkflowBuilder`'s effect depends on this context value by identity, so a fresh object
    // per render would re-run it on every render of this component — inert today only because
    // `useShallow` keeps the selected values referentially stable.
    const hubBuilderContextValue = useMemo(
        () => ({connectionDialogAllowed, includeComponents, sharedConnectionIds}),
        [connectionDialogAllowed, includeComponents, sharedConnectionIds]
    );

    const handleBackClick = () => {
        queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});

        navigate('/embedded/hub');
    };

    return (
        <HubBuilderContext.Provider value={hubBuilderContextValue}>
            <div className="flex size-full flex-col">
                <div className="flex items-center gap-2 border-b px-4 py-2">
                    <Button
                        aria-label="Back to automations"
                        icon={<ArrowLeftIcon />}
                        onClick={handleBackClick}
                        size="icon"
                        variant="ghost"
                    />

                    <span className="text-sm font-medium">{automation?.label}</span>
                </div>

                <div className="relative flex-1">
                    <WorkflowBuilder />
                </div>
            </div>
        </HubBuilderContext.Provider>
    );
};

export default HubBuilderView;
