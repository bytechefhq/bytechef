import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';

export const CANVAS_DRAG_DATA_TYPE = 'application/reactflow';

/**
 * Drag data can only be read on drop, but its types are visible during dragenter/dragover, so a trigger drag carries
 * this extra type to let drop zones tell it apart from a task drag while it is still in flight.
 */
export const TRIGGER_DRAG_DATA_TYPE = 'application/x-bytechef-trigger';

/**
 * A components panel drag carries one of these per cluster element type the component has (e.g. MODEL, TOOLS), so a
 * cluster element placeholder can refuse a component without an element of its type while the drag is in flight.
 * Drag data types are lowercased by the browser, hence the lowercased type name.
 */
const CLUSTER_ELEMENT_TYPE_DRAG_DATA_TYPE_PREFIX = 'application/x-bytechef-cluster-element-type-';

export function getClusterElementTypeDragDataType(clusterElementTypeName: string): string {
    return `${CLUSTER_ELEMENT_TYPE_DRAG_DATA_TYPE_PREFIX}${clusterElementTypeName.toLowerCase()}`;
}

export type CanvasDragKindType = 'task' | 'trigger';

export function getCanvasDragKind(dataTransfer: Pick<DataTransfer, 'types'>): CanvasDragKindType | undefined {
    const types = Array.from(dataTransfer.types);

    if (!types.includes(CANVAS_DRAG_DATA_TYPE)) {
        return undefined;
    }

    return types.includes(TRIGGER_DRAG_DATA_TYPE) ? 'trigger' : 'task';
}

export function carriesClusterElementType(
    dataTransfer: Pick<DataTransfer, 'types'>,
    clusterElementType: string
): boolean {
    return Array.from(dataTransfer.types).includes(
        getClusterElementTypeDragDataType(convertNameToSnakeCase(clusterElementType))
    );
}
