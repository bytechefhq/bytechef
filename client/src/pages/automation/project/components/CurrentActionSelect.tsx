import {ArrowPathIcon} from '@heroicons/react/24/outline';
import {QuestionMarkCircledIcon} from '@radix-ui/react-icons';
import Select from 'components/Select/Select';
import Tooltip from 'components/Tooltip/Tooltip';
import {ActionDefinitionBasicModel} from 'middleware/core/definition-registry';

const CurrentActionSelect = ({
    actions,
    description,
    handleValueChange,
    loading,
    value,
}: {
    actions: ActionDefinitionBasicModel[] | [];
    description: string | undefined;
    handleValueChange: (value: string) => void;
    loading: boolean;
    value: string;
}) => {
    const singleActionComponent = actions?.length === 1;

    return (
        <div className="flex items-end border-b border-gray-100 p-4">
            {singleActionComponent && !!actions[0] ? (
                <>
                    <div className="flex w-full flex-col">
                        <span className="block text-sm font-medium leading-6">
                            Action
                        </span>

                        <div className="flex flex-col overflow-hidden rounded-md bg-gray-100 px-4 py-2 text-sm font-medium">
                            {actions[0].title}
                        </div>
                    </div>

                    {actions[0].description && (
                        <Tooltip text={actions[0].description}>
                            <QuestionMarkCircledIcon className="my-2 ml-4 mr-2 h-5 w-5 self-end" />
                        </Tooltip>
                    )}
                </>
            ) : (
                <>
                    <Select
                        contentClassName="max-w-select-trigger-width max-h-select-content-available-height-1/2"
                        label="Actions"
                        onValueChange={(value) => handleValueChange(value)}
                        options={actions?.map((action) => ({
                            description: action.description,
                            label: action.title!,
                            value: action.name,
                        }))}
                        value={value}
                        triggerClassName={'w-full bg-gray-100 border-none'}
                    />

                    {loading && (
                        <ArrowPathIcon className="my-2 ml-4 mr-2 h-5 w-5 animate-spin" />
                    )}

                    {description && (
                        <Tooltip text={description}>
                            <QuestionMarkCircledIcon className="my-2 ml-4 mr-2 h-5 w-5" />
                        </Tooltip>
                    )}
                </>
            )}
        </div>
    );
};

export default CurrentActionSelect;
