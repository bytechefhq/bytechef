import useInitialViewport from '@/pages/platform/workflow-editor/hooks/useInitialViewport';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET} from '@/shared/constants';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {flowState, setViewportMock} = vi.hoisted(() => ({
    flowState: {height: 0},
    setViewportMock: vi.fn(),
}));

vi.mock('@xyflow/react', () => ({
    useReactFlow: () => ({setViewport: setViewportMock}),
    useStore: (selector: (state: {height: number}) => number) => selector({height: flowState.height}),
}));

interface RenderPropsI {
    enabled: boolean;
    workflowUuid?: string;
}

const renderInitialViewport = ({enabled = true, workflowUuid = 'workflow-1'}: Partial<RenderPropsI> = {}) =>
    renderHook(
        (props: RenderPropsI) => useInitialViewport({canvasHeight: 650, getViewportOffsetX: () => 0, ...props}),
        {
            initialProps: {enabled, workflowUuid},
        }
    );

describe('useInitialViewport', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        flowState.height = 0;

        useLayoutDirectionStore.setState({layoutDirection: 'LR'});
    });

    it('waits for the flow height before positioning', () => {
        renderInitialViewport();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('positions once the flow height is measured', () => {
        const {rerender} = renderInitialViewport();

        flowState.height = 700;

        rerender({enabled: true, workflowUuid: 'workflow-1'});

        expect(setViewportMock).toHaveBeenCalledTimes(1);
        expect(setViewportMock).toHaveBeenCalledWith({x: CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET, y: 25, zoom: 1});
    });

    it('leaves the viewport alone on later resizes of the same workflow', () => {
        flowState.height = 700;

        const {rerender} = renderInitialViewport();

        flowState.height = 900;

        rerender({enabled: true, workflowUuid: 'workflow-1'});

        expect(setViewportMock).toHaveBeenCalledTimes(1);
    });

    it('positions again when another workflow opens', () => {
        flowState.height = 700;

        const {rerender} = renderInitialViewport();

        rerender({enabled: true, workflowUuid: 'workflow-2'});

        expect(setViewportMock).toHaveBeenCalledTimes(2);
    });

    it('positions a workflow without a uuid', () => {
        flowState.height = 700;

        renderHook(() => useInitialViewport({canvasHeight: 650, enabled: true, getViewportOffsetX: () => 0}));

        expect(setViewportMock).toHaveBeenCalledTimes(1);
    });

    it('does nothing when disabled', () => {
        flowState.height = 700;

        renderInitialViewport({enabled: false});

        expect(setViewportMock).not.toHaveBeenCalled();
    });
});
