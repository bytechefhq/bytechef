import Button from '@/components/Button/Button';
import ClusterElementTestPropertiesPopover from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/ClusterElementTestPropertiesPopover';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {useCallback, useState} from 'react';

interface ClusterElementTestButtonProps {
    clusterElementType?: string;
    connectionMissing: boolean;
    currentNode: NodeDataType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onSubmit: (inputParameters: Record<string, any>, onSuccess?: () => void) => void;
    properties: PropertyAllType[];
    saving: boolean;
}

const ClusterElementTestButton = ({
    clusterElementType,
    connectionMissing,
    currentNode,
    onSubmit,
    properties,
    saving,
}: ClusterElementTestButtonProps) => {
    const [open, setOpen] = useState(false);

    const handleSubmit = useCallback(
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (inputParameters: Record<string, any>) => {
            onSubmit(inputParameters, () => setOpen(false));
        },
        [onSubmit]
    );

    return (
        <ClusterElementTestPropertiesPopover
            currentNode={currentNode}
            onOpenChange={setOpen}
            onSubmit={handleSubmit}
            open={open}
            properties={properties}
        >
            <Button
                disabled={connectionMissing || saving}
                label={`Test ${clusterElementType === 'tools' ? 'Tool' : 'Action'}`}
                variant="outline"
            />
        </ClusterElementTestPropertiesPopover>
    );
};

export default ClusterElementTestButton;
