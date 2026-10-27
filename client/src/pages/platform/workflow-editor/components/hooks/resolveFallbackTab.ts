import {TabNameType} from '@/shared/types';

interface ResolveFallbackTabProps {
    actionFetched: boolean;
    activeTab: string;
    componentName?: string;
    connectionsCount: number;
    operationDataMissing: boolean;
    operationDefinition?: {properties?: Array<unknown>};
    operationProperties?: Array<unknown>;
    showOutputTab: boolean;
    taskDispatcher?: boolean;
    taskDispatcherDefinition?: {properties?: Array<unknown>};
    trigger?: boolean;
    triggerFetched: boolean;
}

export default function resolveFallbackTab({
    actionFetched,
    activeTab,
    componentName,
    connectionsCount,
    operationDataMissing,
    operationDefinition,
    operationProperties,
    showOutputTab,
    taskDispatcher,
    taskDispatcherDefinition,
    trigger,
    triggerFetched,
}: ResolveFallbackTabProps): TabNameType | undefined {
    if (activeTab === 'connection' && connectionsCount === 0) {
        return 'description';
    }

    if (componentName === 'manual') {
        return 'description';
    }

    if (
        activeTab === 'properties' &&
        ((!trigger && actionFetched) || (trigger && triggerFetched)) &&
        !operationProperties
    ) {
        return 'description';
    }

    if (activeTab === 'output' && !showOutputTab) {
        return 'description';
    }

    if (
        activeTab === 'properties' &&
        !operationDataMissing &&
        operationDefinition &&
        !operationDefinition.properties?.length
    ) {
        return 'description';
    }

    if (
        activeTab === 'properties' &&
        taskDispatcher &&
        taskDispatcherDefinition &&
        !taskDispatcherDefinition.properties?.length
    ) {
        return 'description';
    }

    return undefined;
}
