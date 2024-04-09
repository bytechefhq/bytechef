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
                        'group flex items-center justify-center',
                        objectProperty ? 'absolute right-0' : 'mx-2'
                    )}
                >
                    <button
                        className={twMerge('p-2', objectProperty && 'p-1')}
                        onClick={() => deleteProperty({propertyName, subPropertyIndex, subPropertyName})}
                    >
                        <XIcon className="size-4 cursor-pointer group-hover:text-red-500" />
                    </button>
                </div>
            </TooltipTrigger>

            <TooltipContent>{`Delete ${objectProperty ? 'property' : 'item'}`}</TooltipContent>
        </Tooltip>
    );
};

export default DeletePropertyButton;
