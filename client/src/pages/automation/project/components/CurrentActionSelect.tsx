import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import Select from 'components/Select/Select';
import Tooltip from 'components/Tooltip/Tooltip';
import {ActionDefinitionBasicModel} from 'middleware/core/definition-registry';

const CurrentActionSelect = ({
    actions,
    description,
    handleValueChange,
    value,
}: {
    actions: ActionDefinitionBasicModel[] | [];
    handleValueChange: (value: string) => void;
    value: string;
    description?: string;
}) => (
    <div className="flex items-end border-b border-gray-100 p-4">
        {actions?.length === 1 && !!actions[0] ? (
            <div className="flex w-full flex-col">
                <div className="flex items-center space-x-1">
                    <span className="text-sm font-medium leading-6">
                        Action
                    </span>

                    {description && (
                        <Tooltip text={description}>
                            <QuestionMarkCircledIcon />
                        </Tooltip>
                    )}
                </div>

                <div className="flex flex-col overflow-hidden rounded-md bg-gray-100 px-4 py-2 text-sm font-medium">
                    {actions[0].title}
                </div>
            </div>
        ) : (
            <Select
                contentClassName="max-w-select-trigger-width max-h-select-content-available-height-1/2"
                description={description}
                label="Actions"
                onValueChange={(value) => handleValueChange(value)}
                options={actions?.map((action) => ({
                    description: action.description,
                    label: action.title!,
                    value: action.name,
                }))}
                value={value}
                triggerClassName="w-full bg-gray-100 border-none"
            />
        )}
    </div>
);

export default CurrentActionSelect;
