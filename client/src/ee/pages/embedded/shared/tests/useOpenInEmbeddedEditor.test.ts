import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {DEVELOPMENT_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useOpenInEmbeddedEditor from '../useOpenInEmbeddedEditor';

const {navigateMock, queryMock, toastMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    queryMock: vi.fn(),
    toastMock: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueryClient: () => ({query: queryMock}),
    };
});

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();

    return {
        ...actual,
        useNavigate: () => navigateMock,
    };
});

vi.mock('sonner', () => ({toast: toastMock}));

describe('useOpenInEmbeddedEditor', () => {
    beforeEach(() => {
        queryMock.mockReset();
        navigateMock.mockReset();
        toastMock.mockReset();

        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
    });

    it('switches to Development before opening an automation workflow from another environment', () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        navigateMock.mockImplementation(() => {
            expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
        });

        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        act(() => result.current.openAutomationWorkflow('workflow-uuid'));

        expect(navigateMock).toHaveBeenCalledWith('/embedded/automation-workflows/workflow-uuid/editor');
    });

    it('stays put with a toast when the project has no workflows', () => {
        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        act(() => result.current.openAutomationWorkflow(undefined));

        expect(toastMock).toHaveBeenCalledWith('The project has no workflows to open.');
        expect(navigateMock).not.toHaveBeenCalled();
    });

    it('opens the first workflow of the integration', () => {
        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        act(() => result.current.openIntegration({id: 5, integrationWorkflowIds: [31, 32]} as Integration));

        expect(navigateMock).toHaveBeenCalledWith('/embedded/integrations/5/integration-workflows/31');
    });

    it('stays put with a toast when the integration has no workflows', () => {
        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        act(() => result.current.openIntegration({id: 5, integrationWorkflowIds: []} as unknown as Integration));

        expect(toastMock).toHaveBeenCalledWith('The integration has no workflows to open.');
        expect(navigateMock).not.toHaveBeenCalled();
    });

    it("opens the latest integration version's workflow matching the configured workflow's uuid", async () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        queryMock.mockResolvedValue([
            {integrationWorkflowId: 41, workflowUuid: 'other-uuid'},
            {integrationWorkflowId: 42, workflowUuid: 'configured-uuid'},
        ]);

        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        await act(() => result.current.openIntegrationWorkflow(5, 'configured-uuid'));

        expect(navigateMock).toHaveBeenCalledWith('/embedded/integrations/5/integration-workflows/42');
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
    });

    it('stays put with a toast when the workflow is gone from the latest integration version', async () => {
        queryMock.mockResolvedValue([{integrationWorkflowId: 41, workflowUuid: 'other-uuid'}]);

        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        await act(() => result.current.openIntegrationWorkflow(5, 'configured-uuid'));

        expect(toastMock).toHaveBeenCalledWith('This workflow no longer exists in the latest integration version.');
        expect(navigateMock).not.toHaveBeenCalled();
    });

    it('keeps the environment and does not navigate when the lookup fails', async () => {
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});

        queryMock.mockRejectedValue(new Error('network'));

        const {result} = renderHook(() => useOpenInEmbeddedEditor());

        await act(() => result.current.openIntegrationWorkflow(5, 'configured-uuid'));

        expect(navigateMock).not.toHaveBeenCalled();
        expect(environmentStore.getState().currentEnvironmentId).toBe(STAGING_ENVIRONMENT);
    });
});
