export const CANVAS_DRAG_DATA_TYPE = 'application/reactflow';

/**
 * Drag data can only be read on drop, but its types are visible during dragenter/dragover, so a trigger drag carries
 * this extra type to let drop zones tell it apart from a task drag while it is still in flight.
 */
export const TRIGGER_DRAG_DATA_TYPE = 'application/x-bytechef-trigger';

export type CanvasDragKindType = 'task' | 'trigger';

export function getCanvasDragKind(dataTransfer: Pick<DataTransfer, 'types'>): CanvasDragKindType | undefined {
    const types = Array.from(dataTransfer.types);

    if (!types.includes(CANVAS_DRAG_DATA_TYPE)) {
        return undefined;
    }

    return types.includes(TRIGGER_DRAG_DATA_TYPE) ? 'trigger' : 'task';
}
