import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {ToolInvocationsWorkspaceScopeI, useToolInvocations} from './useToolInvocations';

const hoisted = vi.hoisted(() => ({
    useToolInvocationLogsQueryMock: vi.fn(),
    useWorkspaceToolInvocationLogsQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useToolInvocationLogsQuery: hoisted.useToolInvocationLogsQueryMock,
    useWorkspaceToolInvocationLogsQuery: hoisted.useWorkspaceToolInvocationLogsQueryMock,
}));

const emptyQueryResult = () => ({
    data: undefined,
    error: null,
    isFetching: false,
    isLoading: false,
    refetch: vi.fn(),
});

const renderUseToolInvocations = (workspaceScope?: ToolInvocationsWorkspaceScopeI) =>
    renderHook(() => useToolInvocations(workspaceScope));

describe('useToolInvocations', () => {
    beforeEach(() => {
        hoisted.useToolInvocationLogsQueryMock.mockReset();
        hoisted.useToolInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReset();
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReturnValue(emptyQueryResult());
    });

    it('queries the tenant-wide log and leaves the workspace query off without a workspace scope', () => {
        renderUseToolInvocations();

        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({page: 0, type: 'AUTOMATION'}),
            {
                enabled: true,
            }
        );
        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {
            enabled: false,
        });
    });

    it('queries the workspace log with the workspace and environment of the scope', () => {
        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReturnValue({
            ...emptyQueryResult(),
            data: {
                workspaceToolInvocationLogs: {
                    content: [{id: '1', toolName: 'CRM__SEARCH_RECORDS'}],
                    number: 0,
                    size: 50,
                    totalElements: 1,
                    totalPages: 1,
                },
            },
        });

        const {result} = renderUseToolInvocations({environmentId: 2, workspaceId: 1049});

        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({environmentId: '2', page: 0, type: 'AUTOMATION', workspaceId: '1049'}),
            {enabled: true}
        );
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
        expect(result.current.toolInvocationLogs).toEqual([{id: '1', toolName: 'CRM__SEARCH_RECORDS'}]);
        expect(result.current.toolInvocationLogPage?.totalElements).toBe(1);
    });

    it('passes the filters to the workspace query and resets the page when one changes', () => {
        const {result} = renderUseToolInvocations({environmentId: 2, workspaceId: 1049});

        act(() => {
            result.current.handlePaginationClick(3);
        });

        act(() => {
            result.current.handleMcpServerChange({label: 'Server', value: '9'});
        });

        expect(result.current.filterPageNumber).toBe(0);
        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({environmentId: '2', mcpServerId: 9, page: 0, workspaceId: '1049'}),
            {enabled: true}
        );
    });

    it('waits for the workspace instead of falling back to the tenant-wide log', () => {
        renderUseToolInvocations({environmentId: 2, workspaceId: undefined});

        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {
            enabled: false,
        });
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenCalledWith(expect.anything(), {enabled: false});
    });

    it('reads the tenant-wide page and its fetch state when there is no workspace scope', () => {
        const refetch = vi.fn();

        hoisted.useToolInvocationLogsQueryMock.mockReturnValue({
            data: {
                toolInvocationLogs: {
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

        const {result} = renderUseToolInvocations();

        expect(result.current.toolInvocationLogs).toEqual([{id: '5', toolName: 'slack_sendMessage'}]);
        expect(result.current.toolInvocationLogPage?.totalElements).toBe(1);
        expect(result.current.toolInvocationLogsIsFetching).toBe(true);
        expect(result.current.toolInvocationLogsIsLoading).toBe(false);

        result.current.refetchToolInvocations();

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('reports the loading state and the error of the workspace query', () => {
        const error = new Error('Access denied');

        hoisted.useWorkspaceToolInvocationLogsQueryMock.mockReturnValue({
            ...emptyQueryResult(),
            error,
            isLoading: true,
        });

        const {result} = renderUseToolInvocations({environmentId: 2, workspaceId: 1049});

        expect(result.current.toolInvocationLogsError).toBe(error);
        expect(result.current.toolInvocationLogsIsLoading).toBe(true);
        expect(result.current.toolInvocationLogs).toEqual([]);
        expect(result.current.toolInvocationLogPage).toBeUndefined();
    });

    it('passes the surface, outcome and date filters as query variables and clears them again', () => {
        const startDate = new Date(Date.UTC(2026, 9, 1));
        const endDate = new Date(2026, 9, 31);

        const {result} = renderUseToolInvocations();

        act(() => {
            result.current.handlePaginationClick(2);
        });

        act(() => {
            result.current.handleSurfaceChange({label: 'Automation MCP', value: 'MCP_AUTOMATION'});
            result.current.handleOutcomeChange({label: 'Error', value: 'ERROR'});
            result.current.handleStartDateChange(startDate);
            result.current.handleEndDateChange(endDate);
        });

        expect(result.current.filterSurface).toBe('MCP_AUTOMATION');
        expect(result.current.filterOutcome).toBe('ERROR');
        expect(result.current.filterStartDate).toBe(startDate);
        expect(result.current.filterEndDate).toBe(endDate);
        expect(result.current.filterPageNumber).toBe(0);
        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            {
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

        expect(hoisted.useToolInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            {
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
        const {result} = renderUseToolInvocations({environmentId: 2, workspaceId: 1049});

        act(() => {
            result.current.handlePaginationClick(4);
        });

        expect(result.current.filterPageNumber).toBe(4);
        expect(hoisted.useWorkspaceToolInvocationLogsQueryMock).toHaveBeenLastCalledWith(
            expect.objectContaining({page: 4, type: 'AUTOMATION'}),
            {enabled: true}
        );
    });
});
