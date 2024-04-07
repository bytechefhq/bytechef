import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import saveWorkflowDefinition from '@/pages/automation/project/utils/saveWorkflowDefinition';
import {ComponentDataType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface DeletePropertyButtonProps {
    currentComponentData: ComponentDataType;
    handleDeletePropertyClick: () => void;
    objectProperty?: boolean;
    propertyName: string;
    subPropertyIndex?: number;
    subPropertyName?: string;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const DeletePropertyButton = ({
    currentComponentData,
    handleDeletePropertyClick,
    objectProperty = true,
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
                <div
                    className={twMerge(
                        'flex items-center justify-center',
                        objectProperty && 'pl-2 pr-1',
                        !objectProperty && 'px-2.5'
                    )}
                >
                    <button onClick={() => deleteProperty({propertyName, subPropertyIndex, subPropertyName})}>
                        <XIcon className="size-[16px] cursor-pointer hover:text-red-500" />
                    </button>
                </div>
            </TooltipTrigger>

            <TooltipContent>{`Delete ${objectProperty ? 'property' : 'item'}`}</TooltipContent>
        </Tooltip>
    );
};

export default DeletePropertyButton;
