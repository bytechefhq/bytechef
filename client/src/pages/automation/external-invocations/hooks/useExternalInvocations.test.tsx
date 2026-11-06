import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    ExternalInvocationsWorkspaceScopeI,
    UseExternalInvocationsOptionsI,
    useExternalInvocations,
} from './useExternalInvocations';

const hoisted = vi.hoisted(() => ({
    useExternalInvocationLogsQueryMock: vi.fn(),
    useWorkspaceExternalInvocationLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useExternalInvocationLogsQuery: hoisted.useExternalInvocationLogsQueryMock,
    useWorkspaceExternalInvocationLogsQuery: hoisted.useWorkspaceExternalInvocationLogsQueryMock,
}));

const emptyQueryResult = () => ({
    data: undefined,
    error: null,
    isFetching: false,
    isLoading: false,
    refetch: vi.fn(),
});

const renderUseExternalInvocations = (workspaceScope?: ExternalInvocationsWorkspaceScopeI) =>
    renderHook(() =>
        useExternalInvocations({type: 'AUTOMATION' as UseExternalInvocationsOptionsI['type'], workspaceScope})
    );

const renderUseEmbeddedExternalInvocations = (environmentId?: number) =>
    renderHook(() =>
        useExternalInvocations({environmentId, type: 'EMBEDDED' as UseExternalInvocationsOptionsI['type']})
    );

describe('useExternalInvocations', () => {
    beforeEach(() => {
        hoisted.useExternalInvocationLogsQueryMock.mockReset();
        hoisted.useExternalInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReset();
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
    });

    it('queries the tenant-wide log and leaves the workspace query off without a workspace scope', () => {
        renderUseExternalInvocations();

        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({page: 0, type: 'AUTOMATION'}),
            {
                enabled: true,
            }
        );
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {
            enabled: false,
        });
    });

    it('queries the workspace log with the workspace and environment of the scope', () => {
        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            ...emptyQueryResult(),
            data: {
                workspaceExternalInvocationLogs: {
                    content: [{id: '1', toolName: 'CRM__SEARCH_RECORDS'}],
                    number: 0,
                    size: 50,
                    totalElements: 1,
                    totalPages: 1,
                },
            },
        });

        const {result} = renderUseExternalInvocations({environmentId: 2, workspaceId: 1049});

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({environmentId: '2', page: 0, type: 'AUTOMATION', workspaceId: '1049'}),
            {enabled: true}
        );
        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(result.current.externalInvocationLogs).toEqual([{id: '1', toolName: 'CRM__SEARCH_RECORDS'}]);
        expect(result.current.externalInvocationLogPage?.totalElements).toBe(1);
    });

    it('passes the filters to the workspace query and resets the page when one changes', () => {
        const {result} = renderUseExternalInvocations({environmentId: 2, workspaceId: 1049});

        act(() => {
            result.current.handlePaginationClick(3);
        });

        act(() => {
            result.current.handleMcpServerChange({label: 'Server', value: '9'});
        });

        expect(result.current.filterPageNumber).toBe(0);
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '2', mcpServerId: 9, page: 0, workspaceId: '1049'}),
            {enabled: true}
        );
    });

    it('passes the A2A server filter to the workspace query and resets the page', () => {
        const {result} = renderUseExternalInvocations({environmentId: 2, workspaceId: 1049});

        act(() => {
            result.current.handlePaginationClick(3);
        });

        act(() => {
            result.current.handleA2aServerChange({label: 'Research agent', value: '4'});
        });

        expect(result.current.filterA2aServerId).toBe('4');
        expect(result.current.filterPageNumber).toBe(0);
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({a2aServerId: 4, environmentId: '2', page: 0, workspaceId: '1049'}),
            {enabled: true}
        );

        act(() => {
            result.current.handleA2aServerChange(undefined);
        });

        expect(result.current.filterA2aServerId).toBeUndefined();
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({a2aServerId: undefined}),
            {enabled: true}
        );
    });

    it('clears the server filter the selected surface cannot have', () => {
        const {result} = renderUseExternalInvocations({environmentId: 2, workspaceId: 1049});

        act(() => {
            result.current.handleA2aServerChange({label: 'Research agent', value: '4'});
            result.current.handleMcpServerChange({label: 'Sales tools', value: '11'});
        });

        act(() => {
            result.current.handleSurfaceChange({label: 'A2A', value: 'A2A'});
        });

        expect(result.current.filterA2aServerId).toBe('4');
        expect(result.current.filterMcpServerId).toBeUndefined();

        act(() => {
            result.current.handleMcpServerChange({label: 'Sales tools', value: '11'});
            result.current.handleSurfaceChange({label: 'MCP', value: 'MCP_AUTOMATION'});
        });

        expect(result.current.filterA2aServerId).toBeUndefined();
        expect(result.current.filterMcpServerId).toBe('11');
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({a2aServerId: undefined, mcpServerId: 11, surface: 'MCP_AUTOMATION'}),
            {enabled: true}
        );
    });

    it('waits for the workspace instead of falling back to the tenant-wide log', () => {
        renderUseExternalInvocations({environmentId: 2, workspaceId: undefined});

        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {
            enabled: false,
        });
        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
    });

    it('reads the tenant-wide page and its fetch state when there is no workspace scope', () => {
        const refetch = vi.fn();

        hoisted.useExternalInvocationLogsQueryMock.mockReturnValue({
            data: {
                externalInvocationLogs: {
                    content: [{id: '5', toolName: 'slack_sendMessage'}],
                    number: 0,
                    size: 50,
                    totalElements: 1,
                    totalPages: 1,
                },
            },
            error: null,
            isFetching: true,
            isLoading: false,
            refetch,
        });

        const {result} = renderUseExternalInvocations();

        expect(result.current.externalInvocationLogs).toEqual([{id: '5', toolName: 'slack_sendMessage'}]);
        expect(result.current.externalInvocationLogPage?.totalElements).toBe(1);
        expect(result.current.externalInvocationLogsIsFetching).toBe(true);
        expect(result.current.externalInvocationLogsIsLoading).toBe(false);

        result.current.refetchExternalInvocations();

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('reports the loading state and the error of the workspace query', () => {
        const error = new Error('Access denied');

        hoisted.useWorkspaceExternalInvocationLogsQueryMock.mockReturnValue({
            ...emptyQueryResult(),
            error,
            isLoading: true,
        });

        const {result} = renderUseExternalInvocations({environmentId: 2, workspaceId: 1049});

        expect(result.current.externalInvocationLogsError).toBe(error);
        expect(result.current.externalInvocationLogsIsLoading).toBe(true);
        expect(result.current.externalInvocationLogs).toEqual([]);
        expect(result.current.externalInvocationLogPage).toBeUndefined();
    });

    it('passes the surface, outcome and date filters as query variables and clears them again', () => {
        const startDate = new Date(Date.UTC(2026, 9, 1));
        const endDate = new Date(2026, 9, 31);

        const {result} = renderUseExternalInvocations();

        act(() => {
            result.current.handlePaginationClick(2);
        });

        act(() => {
            result.current.handleSurfaceChange({label: 'MCP', value: 'MCP_AUTOMATION'});
            result.current.handleOutcomeChange({label: 'Error', value: 'ERROR'});
            result.current.handleStartDateChange(startDate);
            result.current.handleEndDateChange(endDate);
        });

        expect(result.current.filterSurface).toBe('MCP_AUTOMATION');
        expect(result.current.filterOutcome).toBe('ERROR');
        expect(result.current.filterStartDate).toBe(startDate);
        expect(result.current.filterEndDate).toBe(endDate);
        expect(result.current.filterPageNumber).toBe(0);
        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            {
                a2aServerId: undefined,
                environmentId: undefined,
                fromDate: startDate.getTime(),
                mcpServerId: undefined,
                outcome: 'ERROR',
                page: 0,
                surface: 'MCP_AUTOMATION',
                toDate: new Date(2026, 9, 31, 23, 59, 59, 999).getTime(),
                type: 'AUTOMATION',
            },
            {enabled: true}
        );

        act(() => {
            result.current.handleSurfaceChange(undefined);
            result.current.handleOutcomeChange(undefined);
            result.current.handleMcpServerChange(undefined);
            result.current.handleStartDateChange(undefined);
            result.current.handleEndDateChange(undefined);
        });

        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            {
                a2aServerId: undefined,
                environmentId: undefined,
                fromDate: undefined,
                mcpServerId: undefined,
                outcome: undefined,
                page: 0,
                surface: undefined,
                toDate: undefined,
                type: 'AUTOMATION',
            },
            {enabled: true}
        );
    });

    it('keeps the requested page until a filter changes', () => {
        const {result} = renderUseExternalInvocations({environmentId: 2, workspaceId: 1049});

        act(() => {
            result.current.handlePaginationClick(4);
        });

        expect(result.current.filterPageNumber).toBe(4);
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({page: 4, type: 'AUTOMATION'}),
            {enabled: true}
        );
    });

    it('queries the tenant-wide embedded log of the given environment', () => {
        renderUseEmbeddedExternalInvocations(2);

        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '2', page: 0, type: 'EMBEDDED'}),
            {enabled: true}
        );
        expect(hoisted.useWorkspaceExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(expect.anything(), {
            enabled: false,
        });
    });

    it('sends environment 0 as a filter instead of dropping it', () => {
        renderUseEmbeddedExternalInvocations(0);

        expect(hoisted.useExternalInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '0', type: 'EMBEDDED'}),
            {enabled: true}
        );
    });
});
