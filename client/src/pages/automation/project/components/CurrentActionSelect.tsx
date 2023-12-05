import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CheckIcon, QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import {Item, ItemIndicator, ItemText} from '@radix-ui/react-select';
import {ActionDefinitionBasicModel} from 'middleware/hermes/configuration';
import {twMerge} from 'tailwind-merge';

const ActionSelect = ({
    actions,
    description,
    handleValueChange,
    value,
}: {
    actions: ActionDefinitionBasicModel[];
    handleValueChange: (value: string) => void;
    value: string;
    description?: string;
}) => {
    return (
        <div className="flex w-full flex-col">
            <Label className="mb-1 flex items-center space-x-1">
                <span>Actions</span>

                {description && (
                    <Tooltip>
                        <TooltipTrigger>
                            <QuestionMarkCircledIcon />
                        </TooltipTrigger>

                        <TooltipContent>{description}</TooltipContent>
                    </Tooltip>
                )}
            </Label>

            <Select onValueChange={(value) => handleValueChange(value)} value={value}>
                <SelectTrigger className="w-full border-none bg-gray-100 shadow-none">
                    <SelectValue placeholder="Choose action..." />
                </SelectTrigger>

                <SelectContent className="max-h-select-content-available-height-1/2 max-w-select-trigger-width">
                    {actions?.map((action) => (
                        <Item
                            className={twMerge(
                                'radix-disabled:opacity-50 flex cursor-pointer select-none items-center overflow-hidden rounded-md px-8 py-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none'
                            )}
                            key={action.name}
                            value={action.name}
                        >
                            <span className="absolute right-2 flex h-3.5 w-3.5 items-center justify-center">
                                <ItemIndicator>
                                    <CheckIcon className="h-4 w-4" />
                                </ItemIndicator>
                            </span>

                            <div className="flex flex-col">
                                <ItemText>{action.title || action.name}</ItemText>

                                {action.description && (
                                    <span
                                        className="mt-1 line-clamp-2 w-full text-xs text-gray-500"
                                        title={action.description}
                                    >
                                        {action.description}
                                    </span>
                                )}
                            </div>
                        </Item>
                    ))}
                </SelectContent>
            </Select>
        </div>
    );
};

const CurrentActionSelect = ({
    actions,
    description,
    handleValueChange,
    value,
}: {
    actions: ActionDefinitionBasicModel[];
    handleValueChange: (value: string) => void;
    value: string;
    description?: string;
}) => (
    <div className="flex items-end border-b border-gray-100 p-4">
        {actions?.length === 1 && !!actions[0] ? (
            <div className="flex w-full flex-col">
                <div className="flex items-center space-x-1">
                    <span className="text-sm font-medium leading-6">Action</span>

                    {description && (
                        <Tooltip>
                            <TooltipTrigger>
                                <QuestionMarkCircledIcon />
                            </TooltipTrigger>

                            <TooltipContent>{description}</TooltipContent>
                        </Tooltip>
                    )}
                </div>

                <div className="flex flex-col overflow-hidden rounded-md bg-gray-100 px-4 py-2 text-sm font-medium">
                    {actions[0].title}
                </div>
            </div>
        ) : (
            <ActionSelect
                actions={actions}
                description={description}
                handleValueChange={handleValueChange}
                value={value}
            />
        )}
    </div>
);

export default CurrentActionSelect;
