import {HubBuilderContext} from '@/ee/pages/embedded/automation-hub/hubBuilderContext';
import {AutomationHubKeys, useGetWorkflowQuery} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import WorkflowBuilder from '@/ee/pages/embedded/workflow-builder/WorkflowBuilder';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const HubBuilderView = () => {
    const {connectionDialogAllowed, includeComponents} = useAutomationHubStore(
        useShallow((state) => ({
            connectionDialogAllowed: state.connectionDialogAllowed,
            includeComponents: state.includeComponents,
        }))
    );

    const {workflowUuid} = useParams();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const {error: workflowError} = useGetWorkflowQuery(workflowUuid);

    const handleBackClick = useCallback(() => {
        void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});

        void navigate('/embedded/hub');
    }, [navigate, queryClient]);

    const hubBuilderContextValue = useMemo(
        () => ({connectionDialogAllowed, includeComponents, onBack: handleBackClick}),
        [connectionDialogAllowed, handleBackClick, includeComponents]
    );

    useEffect(() => {
        if (workflowError) {
            void navigate('/embedded/hub', {replace: true});
        }
    }, [navigate, workflowError]);

    return (
        <HubBuilderContext.Provider value={hubBuilderContextValue}>
            <div className="relative size-full">
                <WorkflowBuilder />
            </div>
        </HubBuilderContext.Provider>
    );
};

export default HubBuilderView;
