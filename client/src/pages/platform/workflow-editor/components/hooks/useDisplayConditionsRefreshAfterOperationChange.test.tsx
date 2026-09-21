import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {type MockInstance, afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {DisplayConditionsQueryTargetType} from './resolveDisplayConditionsQueryTarget';
import useDisplayConditionsRefreshAfterOperationChange from './useDisplayConditionsRefreshAfterOperationChange';

interface HookPropsI {
    activeTab: string;
    displayConditionsDataUpdatedAt: number;
    displayConditionsErrorUpdatedAt: number;
    displayConditionsQueryTarget: DisplayConditionsQueryTargetType;
    nodeName: string | undefined;
    nodeType: string | undefined;
    operationChangeInProgress: boolean;
    workflowId: string;
}

const INITIAL_PROPS: HookPropsI = {
    activeTab: 'properties',
    displayConditionsDataUpdatedAt: 1_000,
    displayConditionsErrorUpdatedAt: 0,
    displayConditionsQueryTarget: 'regular',
    nodeName: 'mistral_1',
    nodeType: 'mistral/v1/uploadFile',
    operationChangeInProgress: false,
    workflowId: 'workflow-1',
};

const NOW = 10_000;

describe('useDisplayConditionsRefreshAfterOperationChange', () => {
    let queryClient: QueryClient;
    let resetQueriesSpy: MockInstance<QueryClient['resetQueries']>;

    beforeEach(() => {
        vi.spyOn(Date, 'now').mockReturnValue(NOW);

        queryClient = new QueryClient();

        resetQueriesSpy = vi.spyOn(queryClient, 'resetQueries').mockResolvedValue();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    const renderRefreshHook = (initialProps: HookPropsI = INITIAL_PROPS) =>
        renderHook((props: HookPropsI) => useDisplayConditionsRefreshAfterOperationChange(props), {
            initialProps,
            wrapper: ({children}: {children: ReactNode}) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

    it('should not report loading or reset anything while no operation switch happens', () => {
        const {result} = renderRefreshHook();

        expect(result.current).toBe(false);
        expect(resetQueriesSpy).not.toHaveBeenCalled();
    });

    it('should stay loading after the switch is saved until display conditions newer than the reset arrive', () => {
        const {rerender, result} = renderRefreshHook();

        rerender({...INITIAL_PROPS, operationChangeInProgress: true});

        expect(result.current).toBe(true);

        rerender({...INITIAL_PROPS, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(true);
        expect(resetQueriesSpy.mock.calls.map(([filters]) => filters?.queryKey)).toEqual([
            ['workflowNodeParameters', 'workflow-1'],
            ['clusterElementParameters', 'workflow-1'],
        ]);

        rerender({...INITIAL_PROPS, displayConditionsDataUpdatedAt: NOW + 1, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(false);
    });

    it('should clear loading for a cluster element when its display conditions query fails after the reset', () => {
        const clusterProps: HookPropsI = {...INITIAL_PROPS, displayConditionsQueryTarget: 'cluster'};

        const {rerender, result} = renderRefreshHook(clusterProps);

        rerender({...clusterProps, operationChangeInProgress: true});
        rerender({...clusterProps, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(true);

        rerender({...clusterProps, displayConditionsErrorUpdatedAt: NOW + 1, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(false);
    });

    it('should reset again for a switch saved after an earlier one already refreshed', () => {
        const {rerender, result} = renderRefreshHook();

        rerender({...INITIAL_PROPS, operationChangeInProgress: true});
        rerender({...INITIAL_PROPS, nodeType: 'mistral/v1/ocr'});
        rerender({...INITIAL_PROPS, displayConditionsDataUpdatedAt: NOW + 1, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(false);

        vi.spyOn(Date, 'now').mockReturnValue(NOW + 5);

        rerender({...INITIAL_PROPS, displayConditionsDataUpdatedAt: NOW + 1, nodeType: 'mistral/v1/chat'});

        expect(result.current).toBe(true);
        expect(resetQueriesSpy).toHaveBeenCalledTimes(4);

        rerender({...INITIAL_PROPS, displayConditionsDataUpdatedAt: NOW + 6, nodeType: 'mistral/v1/chat'});

        expect(result.current).toBe(false);
    });

    it('should keep loading while the Properties tab is closed and clear once its query settles there', () => {
        const otherTabProps: HookPropsI = {
            ...INITIAL_PROPS,
            activeTab: 'description',
            displayConditionsQueryTarget: 'none',
        };

        const {rerender, result} = renderRefreshHook(otherTabProps);

        rerender({...otherTabProps, operationChangeInProgress: true});
        rerender({...otherTabProps, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(true);

        rerender({...INITIAL_PROPS, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(true);

        rerender({...INITIAL_PROPS, displayConditionsDataUpdatedAt: NOW + 1, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(false);
    });

    it('should clear loading on the Properties tab when there is no display conditions query to wait for', () => {
        const noQueryProps: HookPropsI = {...INITIAL_PROPS, displayConditionsQueryTarget: 'none'};

        const {rerender, result} = renderRefreshHook(noQueryProps);

        rerender({...noQueryProps, operationChangeInProgress: true});
        rerender({...noQueryProps, nodeType: 'mistral/v1/ocr'});

        expect(result.current).toBe(false);
    });

    it('should clear loading when a switch ends without saving a new node type', () => {
        const {rerender, result} = renderRefreshHook();

        rerender({...INITIAL_PROPS, operationChangeInProgress: true});
        rerender(INITIAL_PROPS);

        expect(result.current).toBe(false);
        expect(resetQueriesSpy).not.toHaveBeenCalled();
    });

    it('should not reset when a different node is opened', () => {
        const {rerender, result} = renderRefreshHook();

        rerender({...INITIAL_PROPS, nodeName: 'slack_1', nodeType: 'slack/v1/sendMessage'});

        expect(result.current).toBe(false);
        expect(resetQueriesSpy).not.toHaveBeenCalled();
    });
});
