import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useCreateIntegrationWorkflow} from '../useCreateIntegrationWorkflow';

const hoisted = vi.hoisted(() => ({
    captureIntegrationWorkflowCreated: vi.fn(),
    mutationOptions: {current: undefined as {onSuccess?: (integrationWorkflowId: number) => void} | undefined},
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationWorkflowCreated: hoisted.captureIntegrationWorkflowCreated}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: (options: typeof hoisted.mutationOptions.current) => {
        hoisted.mutationOptions.current = options;

        return {mutate: vi.fn()};
    },
}));

const renderUseCreateIntegrationWorkflow = (
    bottomResizablePanelRef?: {current: Pick<PanelImperativeHandle, 'resize'> | null},
    integrationId = 5
) => {
    const queryClient = new QueryClient();
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    renderHook(
        () =>
            useCreateIntegrationWorkflow({
                bottomResizablePanelRef: bottomResizablePanelRef as {current: PanelImperativeHandle | null},
                integrationId,
            }),
        {wrapper}
    );

    return {invalidateQueriesSpy};
};

describe('useCreateIntegrationWorkflow', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.mutationOptions.current = undefined;

        useWorkflowEditorStore.setState({showBottomPanel: true});
    });

    it('refreshes the integration workflow lists and opens the created workflow', () => {
        const resize = vi.fn();
        const {invalidateQueriesSpy} = renderUseCreateIntegrationWorkflow({current: {resize}});

        hoisted.mutationOptions.current?.onSuccess?.(42);

        expect(hoisted.captureIntegrationWorkflowCreated).toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: IntegrationWorkflowKeys.integrationWorkflows(5),
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: IntegrationKeys.integrations});
        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(false);
        expect(resize).toHaveBeenCalledWith(0);
        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/5/integration-workflows/42');
    });

    it('opens the created workflow without a bottom panel to collapse', () => {
        renderUseCreateIntegrationWorkflow(undefined, 8);

        hoisted.mutationOptions.current?.onSuccess?.(3);

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/8/integration-workflows/3');
    });
});
