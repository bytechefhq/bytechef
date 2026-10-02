import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE} from '../../../utils/canvasDragData';
import WorkflowNodesTabsItem from '../WorkflowNodesTabsItem';

type NodePropType = Parameters<typeof WorkflowNodesTabsItem>[0]['node'];

const startDrag = (node: Partial<NodePropType>) => {
    render(<WorkflowNodesTabsItem draggable node={{name: 'schedule', title: 'Schedule', ...node} as NodePropType} />);

    const dataTransfer = {setData: vi.fn(), setDragImage: vi.fn()};

    fireEvent.dragStart(screen.getByRole('listitem'), {dataTransfer});

    return dataTransfer.setData;
};

describe('WorkflowNodesTabsItem', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    beforeEach(() => {
        vi.stubGlobal(
            'IntersectionObserver',
            class {
                disconnect = vi.fn();
                observe = vi.fn();
            }
        );
    });

    it('marks a trigger drag so drop zones can tell it apart while it is in flight', () => {
        const setData = startDrag({trigger: true});

        expect(setData).toHaveBeenCalledWith(CANVAS_DRAG_DATA_TYPE, 'schedule--trigger');
        expect(setData).toHaveBeenCalledWith(TRIGGER_DRAG_DATA_TYPE, 'schedule--trigger');
    });

    it('does not mark a task drag as a trigger drag', () => {
        const setData = startDrag({name: 'logger', trigger: false});

        expect(setData).toHaveBeenCalledWith(CANVAS_DRAG_DATA_TYPE, 'logger');
        expect(setData).not.toHaveBeenCalledWith(TRIGGER_DRAG_DATA_TYPE, expect.anything());
    });
});
