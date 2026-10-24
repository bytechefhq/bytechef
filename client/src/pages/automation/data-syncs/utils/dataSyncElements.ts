import {DataSyncElementKind} from '@/shared/middleware/graphql';

interface ElementLikeI {
    kind: DataSyncElementKind;
}

interface DataSyncLikeI<T extends ElementLikeI> {
    elements?: Array<T | null> | null;
}

export function findElement<T extends ElementLikeI>(
    dataSync: DataSyncLikeI<T>,
    kind: DataSyncElementKind
): T | undefined {
    return (dataSync.elements ?? []).find((element): element is T => element != null && element.kind === kind);
}

export function elementKindKey(kind: DataSyncElementKind): 'destination' | 'processor' | 'source' {
    if (kind === DataSyncElementKind.Source) {
        return 'source';
    }

    if (kind === DataSyncElementKind.Destination) {
        return 'destination';
    }

    return 'processor';
}

export const PROCESSOR_COMPONENT_NAME = 'dataStreamProcessor';
export const PROCESSOR_COMPONENT_VERSION = 1;
export const PROCESSOR_OPERATION_NAME = 'fieldMapper';

export const DATA_SYNC_TASK_NODE_NAME = 'dataStream_1';

export function elementNodeName(kind: DataSyncElementKind): string {
    return `${elementKindKey(kind)}_1`;
}
