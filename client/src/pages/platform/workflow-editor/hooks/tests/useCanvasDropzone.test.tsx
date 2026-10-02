import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {CANVAS_DRAG_DATA_TYPE, CanvasDragKindType, TRIGGER_DRAG_DATA_TYPE} from '../../utils/canvasDragData';
import useCanvasDropzone from '../useCanvasDropzone';

const Dropzone = ({acceptedDragKind}: {acceptedDragKind: CanvasDragKindType}) => {
    const {dropzoneHandlers, isDropzoneActive} = useCanvasDropzone(acceptedDragKind);

    return (
        <div data-active={isDropzoneActive} data-testid="dropzone" {...dropzoneHandlers}>
            <span data-testid="dropzone-child" />
        </div>
    );
};

const fireDragLeave = (element: Element, relatedTarget: Element) =>
    fireEvent(element, new MouseEvent('dragleave', {bubbles: true, relatedTarget}));

const createDataTransfer = (dragKind: CanvasDragKindType) => ({
    types: dragKind === 'trigger' ? [CANVAS_DRAG_DATA_TYPE, TRIGGER_DRAG_DATA_TYPE] : [CANVAS_DRAG_DATA_TYPE],
});

describe('useCanvasDropzone', () => {
    it('activates for a drag of the accepted kind', () => {
        render(<Dropzone acceptedDragKind="task" />);

        fireEvent.dragEnter(screen.getByTestId('dropzone'), {dataTransfer: createDataTransfer('task')});

        expect(screen.getByTestId('dropzone')).toHaveAttribute('data-active', 'true');
    });

    it('accepts a drop and re-arms while a drag of the accepted kind moves over it', () => {
        render(<Dropzone acceptedDragKind="trigger" />);

        const dropzone = screen.getByTestId('dropzone');

        const dragOverAllowed = fireEvent.dragOver(dropzone, {dataTransfer: createDataTransfer('trigger')});

        expect(dragOverAllowed).toBe(false);
        expect(dropzone).toHaveAttribute('data-active', 'true');

        fireEvent.drop(dropzone, {dataTransfer: createDataTransfer('trigger')});

        expect(dropzone).toHaveAttribute('data-active', 'false');
    });

    it('ignores a drag of another kind', () => {
        render(<Dropzone acceptedDragKind="task" />);

        fireEvent.dragEnter(screen.getByTestId('dropzone'), {dataTransfer: createDataTransfer('trigger')});
        fireEvent.dragOver(screen.getByTestId('dropzone'), {dataTransfer: createDataTransfer('trigger')});

        expect(screen.getByTestId('dropzone')).toHaveAttribute('data-active', 'false');
    });

    it('stays active while the drag moves onto a child element', () => {
        render(<Dropzone acceptedDragKind="trigger" />);

        const dropzone = screen.getByTestId('dropzone');

        fireEvent.dragEnter(dropzone, {dataTransfer: createDataTransfer('trigger')});
        fireDragLeave(dropzone, screen.getByTestId('dropzone-child'));

        expect(dropzone).toHaveAttribute('data-active', 'true');
    });

    it('deactivates when the drag leaves the dropzone or ends anywhere', () => {
        render(<Dropzone acceptedDragKind="task" />);

        const dropzone = screen.getByTestId('dropzone');

        fireEvent.dragEnter(dropzone, {dataTransfer: createDataTransfer('task')});
        fireDragLeave(dropzone, document.body);

        expect(dropzone).toHaveAttribute('data-active', 'false');

        fireEvent.dragEnter(dropzone, {dataTransfer: createDataTransfer('task')});
        fireEvent.dragEnd(document);

        expect(dropzone).toHaveAttribute('data-active', 'false');
    });
});
