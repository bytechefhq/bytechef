import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    getProjectDeploymentsRefetchInterval,
    useGetWorkspaceProjectDeploymentsQuery,
} from '../projectDeployments.queries';

const hoisted = vi.hoisted(() => ({
    getWorkspaceProjectDeploymentsMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/automation/configuration')>()),
    ProjectDeploymentApi: class {
        getWorkspaceProjectDeployments = hoisted.getWorkspaceProjectDeploymentsMock;
    },
}));

const now = new Date('2026-11-25T10:50:00Z').getTime();

const secondsAgo = (seconds: number) => new Date(now - seconds * 1000);

describe('getProjectDeploymentsRefetchInterval', () => {
    it('does not poll without deployments', () => {
        expect(getProjectDeploymentsRefetchInterval(undefined, now)).toBe(false);
        expect(getProjectDeploymentsRefetchInterval([], now)).toBe(false);
    });

    it('does not poll when every execution has finished', () => {
        const projectDeployments = [
            {
                lastExecutionDate: secondsAgo(5),
                lastExecutionStartDate: secondsAgo(10),
                projectDeploymentWorkflows: [
                    {lastExecutionDate: secondsAgo(5), lastExecutionStartDate: secondsAgo(10)},
                ],
            },
        ] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments, now)).toBe(false);
    });

    it('polls while a deployment execution has started but not ended', () => {
        const projectDeployments = [{lastExecutionStartDate: secondsAgo(10)}] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments, now)).toBe(3000);
    });

    it('polls while a workflow execution has started but not ended', () => {
        const projectDeployments = [
            {
                lastExecutionDate: secondsAgo(5),
                lastExecutionStartDate: secondsAgo(10),
                projectDeploymentWorkflows: [
                    {lastExecutionDate: secondsAgo(5)},
                    {lastExecutionStartDate: secondsAgo(10)},
                ],
            },
        ] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments, now)).toBe(3000);
    });

    it('polls less often the longer the youngest running execution has been running', () => {
        const runningSince = (seconds: number) =>
            getProjectDeploymentsRefetchInterval(
                [{lastExecutionStartDate: secondsAgo(seconds)}] as ProjectDeployment[],
                now
            );

        expect(runningSince(59)).toBe(3000);
        expect(runningSince(60)).toBe(10000);
        expect(runningSince(10 * 60 - 1)).toBe(10000);
        expect(runningSince(10 * 60)).toBe(30000);
        expect(runningSince(60 * 60 - 1)).toBe(30000);
    });

    it('stops polling once the youngest running execution has been running for an hour', () => {
        const projectDeployments = [{lastExecutionStartDate: secondsAgo(60 * 60)}] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments, now)).toBe(false);
    });

    it('uses the youngest running execution when several are running', () => {
        const projectDeployments = [
            {lastExecutionStartDate: secondsAgo(2 * 60 * 60)},
            {lastExecutionStartDate: secondsAgo(30)},
        ] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments, now)).toBe(3000);
    });
});

describe('useGetWorkspaceProjectDeploymentsQuery', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(now);

        hoisted.getWorkspaceProjectDeploymentsMock.mockReset();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('refetches while an execution is running and stops once it has finished', async () => {
        hoisted.getWorkspaceProjectDeploymentsMock
            .mockResolvedValueOnce([{lastExecutionStartDate: new Date(now)}])
            .mockResolvedValue([{lastExecutionDate: new Date(now + 3000), lastExecutionStartDate: new Date(now)}]);

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        const wrapper = ({children}: {children: ReactNode}) =>
            createElement(QueryClientProvider, {client: queryClient}, children);

        const {unmount} = renderHook(() => useGetWorkspaceProjectDeploymentsQuery({id: 1}), {wrapper});

        await act(async () => {
            await vi.advanceTimersByTimeAsync(0);
        });

        expect(hoisted.getWorkspaceProjectDeploymentsMock).toHaveBeenCalledTimes(1);

        await act(async () => {
            await vi.advanceTimersByTimeAsync(3000);
        });

        expect(hoisted.getWorkspaceProjectDeploymentsMock).toHaveBeenCalledTimes(2);

        await act(async () => {
            await vi.advanceTimersByTimeAsync(60 * 1000);
        });

        expect(hoisted.getWorkspaceProjectDeploymentsMock).toHaveBeenCalledTimes(2);

        unmount();

        queryClient.clear();
    });
});
