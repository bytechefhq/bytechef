import ConnectionTabConnectionFieldset from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTabConnectionFieldset';
import {
    ComponentConnection,
    ComponentDefinition,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {useConnectionNoteStore} from '../../../stores/useConnectionNoteStore';

type ConnectionTabPropsType = {
    className?: string;
    componentConnections: Array<ComponentConnection>;
    currentComponentDefinition?: ComponentDefinition;
    workflowNodeName: string;
    workflowId: string;
    workflowTestConfigurationConnections?: Array<WorkflowTestConfigurationConnection>;
};

const ConnectionTab = ({
    className,
    componentConnections,
    currentComponentDefinition,
    workflowId,
    workflowNodeName,
    workflowTestConfigurationConnections,
}: ConnectionTabPropsType) => {
    const {setShowConnectionNote, showConnectionNote} = useConnectionNoteStore(
        useShallow((state) => ({
            setShowConnectionNote: state.setShowConnectionNote,
            showConnectionNote: state.showConnectionNote,
        }))
    );

    return (
        <div className={twMerge('flex h-full flex-col gap-6 overflow-y-auto overflow-x-hidden p-4', className)}>
            {componentConnections.map((componentConnection) => {
                const workflowTestConfigurationConnection = workflowTestConfigurationConnections?.find(
                    (testConfigConnection) => testConfigConnection.workflowConnectionKey === componentConnection.key
                );

                return (
                    <ConnectionTabConnectionFieldset
                        componentConnection={componentConnection}
                        componentConnectionsCount={componentConnections.length}
                        currentComponentDefinition={currentComponentDefinition}
                        key={componentConnection.key}
                        workflowId={workflowId}
                        workflowNodeName={workflowNodeName}
                        workflowTestConfigurationConnection={workflowTestConfigurationConnection}
                    />
                );
            })}

            {showConnectionNote && (
                <div className="flex flex-col rounded-md bg-amber-100 p-4 text-gray-800">
                    <div className="flex items-center pb-2">
                        <span className="font-medium">Note</span>

                        <button
                            className="ml-auto p-0"
                            onClick={() => setShowConnectionNote(false)}
                            title="Close the note"
                        >
                            <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                        </button>
                    </div>

                    <p className="text-sm text-gray-800">
                        The selected connections are used for testing purposes only.
                    </p>
                </div>
            )}
        </div>
    );
};

export default ConnectionTab;
