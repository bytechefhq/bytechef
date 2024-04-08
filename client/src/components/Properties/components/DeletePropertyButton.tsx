import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {UpdateWorkflowRequest, WorkflowModel} from '@/middleware/automation/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import saveWorkflowDefinition from '@/pages/automation/project/utils/saveWorkflowDefinition';
import {ComponentType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface DeletePropertyButtonProps {
    currentComponent: ComponentType;
    handleDeletePropertyClick: () => void;
    objectProperty?: boolean;
    propertyName: string;
    subPropertyIndex?: number;
    subPropertyName?: string;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}

const DeletePropertyButton = ({
    currentComponent,
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
        if (!currentComponent.parameters) {
            return;
        }

        if (subPropertyName) {
            delete currentComponent.parameters[propertyName][subPropertyName];
        } else if (subPropertyIndex !== undefined) {
            currentComponent.parameters[propertyName].splice(subPropertyIndex, 1);
        } else {
            delete currentComponent.parameters[propertyName];
        }

        saveWorkflowDefinition(
            {...currentComponent, name: currentComponent.workflowNodeName},
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
                        objectProperty && 'pl-2 pr-1 absolute right-0 top-1',
                        !objectProperty && 'px-3'
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
