import useFlowCenterOffset from '@/pages/platform/workflow-editor/hooks/useFlowCenterOffset';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useStoreMock} = vi.hoisted(() => ({useStoreMock: vi.fn()}));

vi.mock('@xyflow/react', () => ({
    useStore: (selector: (state: unknown) => unknown) => useStoreMock(selector),
}));

interface BoxI {
    left: number;
    top: number;
    width: number;
}

const createBox = ({left, top, width}: BoxI) => ({
    getBoundingClientRect: () => ({left, right: left + width, top, width}),
});

const createDomNode = (boxes: Array<BoxI>, containerLeft = 0, containerWidth = 1000) => ({
    getBoundingClientRect: () => ({left: containerLeft, width: containerWidth}),
    querySelectorAll: () =>
        boxes.map((box) => ({
            querySelector: (selector: string) => (selector === '[data-node-box]' ? createBox(box) : null),
        })),
});

const createDomNodeWithoutBoxes = () => ({
    getBoundingClientRect: () => ({left: 0, width: 1000}),
    querySelectorAll: () => [{querySelector: () => null}],
});

const mockStore = (domNode: unknown) => {
    useStoreMock.mockImplementation((selector: (state: never) => unknown) =>
        selector({
            domNode,
            nodeLookup: new Map([['node_1', {position: {x: 10}}]]),
            transform: [0, 0, 1],
            width: 1000,
        } as never)
    );
};

describe('useFlowCenterOffset', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useLayoutDirectionStore.setState({layoutDirection: 'TB'});
    });

    it('is zero before the flow has a container', () => {
        mockStore(null);

        const {result} = renderHook(() => useFlowCenterOffset());

        expect(result.current).toBe(0);
    });

    it('is zero when no node renders a box', () => {
        mockStore(createDomNodeWithoutBoxes());

        const {result} = renderHook(() => useFlowCenterOffset());

        expect(result.current).toBe(0);
    });

    it('measures the topmost box top to bottom', () => {
        mockStore(
            createDomNode([
                {left: 600, top: 100, width: 100},
                {left: 200, top: 20, width: 100},
            ])
        );

        const {result} = renderHook(() => useFlowCenterOffset());

        expect(result.current).toBe(-250);
    });

    it('measures the whole row left to right', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        mockStore(
            createDomNode([
                {left: 600, top: 100, width: 100},
                {left: 200, top: 20, width: 100},
            ])
        );

        const {result} = renderHook(() => useFlowCenterOffset());

        expect(result.current).toBe(-50);
    });
});
