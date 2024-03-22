import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import saveWorkflowDefinition from '@/pages/automation/project/utils/saveWorkflowDefinition';
import {ComponentDataType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';

interface DeletePropertyButtonProps {
    currentComponentData: ComponentDataType;
    handleDeletePropertyClick: () => void;
    propertyName: string;
    subPropertyIndex?: number;
    subPropertyName?: string;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const DeletePropertyButton = ({
    currentComponentData,
    handleDeletePropertyClick,
    propertyName,
    subPropertyIndex,
    subPropertyName,
    updateWorkflowMutation,
}: DeletePropertyButtonProps) => {
    const {workflow} = useWorkflowDataStore();

    const deleteProperty = ({
        propertyName,
        subPropertyIndex,
        subPropertyName,
    }: {
        propertyName: string;
        subPropertyName?: string;
        subPropertyIndex?: number;
    }) => {
        if (!currentComponentData.parameters) {
            return;
        }

        if (subPropertyName) {
            delete currentComponentData.parameters[propertyName][subPropertyName];
        } else if (subPropertyIndex !== undefined) {
            currentComponentData.parameters[propertyName].splice(subPropertyIndex, 1);
        } else {
            delete currentComponentData.parameters[propertyName];
        }

        saveWorkflowDefinition(
            {...currentComponentData, name: currentComponentData.workflowNodeName},
            workflow,
            updateWorkflowMutation
        );

        if (handleDeletePropertyClick) {
            handleDeletePropertyClick();
        }
    };

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="ml-1 self-center"
                    onClick={() => deleteProperty({propertyName, subPropertyIndex, subPropertyName})}
                    size="icon"
                    variant="ghost"
                >
                    <XIcon className="size-8 cursor-pointer p-2 hover:text-red-500" />
                </Button>
            </TooltipTrigger>

            <TooltipContent>Delete property</TooltipContent>
        </Tooltip>
    );
};

export default DeletePropertyButton;
