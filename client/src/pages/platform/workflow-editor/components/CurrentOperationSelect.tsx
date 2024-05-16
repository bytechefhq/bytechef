import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {Item, ItemIndicator, ItemText} from '@radix-ui/react-select';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {ActionDefinitionBasicModel, TriggerDefinitionBasicModel} from 'middleware/platform/configuration';
import {twMerge} from 'tailwind-merge';

interface CurrentOperationSelectProps {
    description?: string;
    handleValueChange: (value: string) => void;
    operations: Array<ActionDefinitionBasicModel | TriggerDefinitionBasicModel>;
    triggerSelect?: boolean;
    value: string;
}

const OperationSelect = ({description, handleValueChange, operations, value}: CurrentOperationSelectProps) => (
    <div className="flex w-full flex-col">
        <Label className="flex items-center space-x-1">
            <span className="text-sm font-medium leading-6">Actions</span>

            {description && (
                <Tooltip>
                    <TooltipTrigger>
                        <QuestionMarkCircledIcon />
                    </TooltipTrigger>

                    <TooltipPortal>
                        <TooltipContent className="truncate">{description}</TooltipContent>
                    </TooltipPortal>
                </Tooltip>
            )}
        </Label>

        <Select onValueChange={(value) => handleValueChange(value)} value={value}>
            <SelectTrigger className="w-full border-none bg-gray-100 shadow-none">
                <SelectValue placeholder="Choose action..." />
            </SelectTrigger>

            <SelectContent className="max-h-select-content-available-height-1/2 max-w-select-trigger-width">
                {operations?.map((operation) => (
                    <Item
                        className={twMerge(
                            'radix-disabled:opacity-50 flex cursor-pointer select-none items-center overflow-hidden rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none'
                        )}
                        key={operation.name}
                        value={operation.name}
                    >
                        <span className="absolute right-2 flex size-3.5 items-center justify-center">
                            <ItemIndicator>
                                <CheckIcon className="size-4" />
                            </ItemIndicator>
                        </span>

                        <div className="flex flex-col">
                            <ItemText>{operation.title || operation.name}</ItemText>

                            {operation.description && (
                                <span
                                    className="mt-1 line-clamp-2 w-full text-xs text-gray-500"
                                    title={operation.description}
                                >
                                    {operation.description}
                                </span>
                            )}
                        </div>
                    </Item>
                ))}
            </SelectContent>
        </Select>
    </div>
);

const CurrentOperationSelect = ({
    description,
    handleValueChange,
    operations,
    triggerSelect,
    value,
}: CurrentOperationSelectProps) => (
    <div className="flex items-end border-b border-gray-100 p-4">
        {operations?.length === 1 && !!operations[0] ? (
            <div className="flex w-full flex-col">
                <div className="flex items-center space-x-1">
                    <span className="text-sm font-medium leading-6">{triggerSelect ? 'Triggers' : 'Actions'}</span>

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <QuestionMarkCircledIcon />
                            </TooltipTrigger>

                            <TooltipContent>{description}</TooltipContent>
                        </Tooltip>
                    )}
                </div>

                <span className="flex flex-col overflow-hidden rounded-md bg-gray-100 px-4 py-2 text-sm font-medium">
                    {operations[0].title}
                </span>
            </div>
        ) : (
            <OperationSelect
                description={description}
                handleValueChange={handleValueChange}
                operations={operations}
                value={value}
            />
        )}
    </div>
);

export default CurrentOperationSelect;
