import {useEffect, useRef} from 'react';

import resolveNodeOpenTab from './resolveNodeOpenTab';

interface UseNodeOpenTabProps {
    componentDefinition?: {connectionRequired?: boolean; name?: string};
    connectionsFetched: boolean;
    nodeComponentName?: string;
    onConnectionTabRequired: () => void;
    testConfigurationConnectionsCount: number;
    workflowNodeName?: string;
}

export default function useNodeOpenTab({
    componentDefinition,
    connectionsFetched,
    nodeComponentName,
    onConnectionTabRequired,
    testConfigurationConnectionsCount,
    workflowNodeName,
}: UseNodeOpenTabProps) {
    const resolvedWorkflowNodeNameRef = useRef<string | undefined>(undefined);
    const onConnectionTabRequiredRef = useRef(onConnectionTabRequired);

    onConnectionTabRequiredRef.current = onConnectionTabRequired;

    useEffect(() => {
        if (!workflowNodeName) {
            resolvedWorkflowNodeNameRef.current = undefined;

            return;
        }

        if (resolvedWorkflowNodeNameRef.current === workflowNodeName) {
            return;
        }

        const nodeOpenTabResolution = resolveNodeOpenTab({
            componentDefinition,
            connectionsFetched,
            nodeComponentName,
            testConfigurationConnectionsCount,
        });

        if (nodeOpenTabResolution === 'WAIT') {
            return;
        }

        resolvedWorkflowNodeNameRef.current = workflowNodeName;

        if (nodeOpenTabResolution === 'CONNECTION') {
            onConnectionTabRequiredRef.current();
        }
    }, [
        componentDefinition,
        connectionsFetched,
        nodeComponentName,
        testConfigurationConnectionsCount,
        workflowNodeName,
    ]);
}
