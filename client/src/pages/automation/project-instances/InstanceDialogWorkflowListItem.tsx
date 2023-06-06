import {Switch} from '@headlessui/react';
import Input from 'components/Input/Input';
import Properties from 'components/Properties/Properties';
import TextArea from 'components/TextArea/TextArea';
import {InputModelToJSON, WorkflowModel} from 'middleware/automation/project';
import {ControlTypeModelFromJSON} from 'middleware/core/definition-registry';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

const InstanceDialogWorkflowListItem = ({
    workflowModels,
}: {
    workflowModels: WorkflowModel;
}) => {
    const [isEnabled, setIsEnabled] = useState(false);
    const [selectedTabIndex, setSelectedTabIndex] = useState(0);

    const toggleIsEnabled = () => {
        setIsEnabled(!isEnabled);
    };

    const ConfigurationForm = () => (
        <div className="p-2">
            {workflowModels.inputs && (
                <Properties
                    properties={workflowModels.inputs.map((x) => {
                        let json = InputModelToJSON(x);
                        json.controlType =
                            json.type === 'string' ? 'TEXT' : undefined;

                        return ControlTypeModelFromJSON(json) as PropertyType;
                    })}
                />
            )}
        </div>
    );

    const ConnectionForm = () => (
        <div>
            {workflowModels.connections?.map((x) => (
                <Input
                    label={x.componentName}
                    labelClassName="px-2"
                    name={x.componentName}
                />
            ))}
        </div>
    );

    const tabs = [
        {
            content: <ConfigurationForm />,
            name: 'Configuration',
        },
        {
            content: <ConnectionForm />,
            name: 'Connections',
        },
    ];

    return (
        <span className="mb-4">
            <span className="justfy-start flex pr-20 font-semibold">
                {workflowModels.label}
            </span>

            <Switch
                checked={isEnabled}
                onChange={toggleIsEnabled}
                className={twMerge(
                    'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                    isEnabled ? 'bg-blue-600' : 'bg-gray-200'
                )}
            >
                <span
                    aria-hidden="true"
                    className={twMerge(
                        'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                        isEnabled ? 'translate-x-5' : 'translate-x-0'
                    )}
                />
            </Switch>

            {isEnabled && (
                <div className="hidden border-b border-gray-200 sm:block">
                    <nav className="flex space-x-8" aria-label="Tabs">
                        {tabs.map((tab, index) => (
                            <span
                                key={tab.name}
                                className={twMerge(
                                    'cursor-pointer whitespace-nowrap border-b-2 px-1 py-4 text-sm font-medium',
                                    selectedTabIndex === index
                                        ? 'border-blue-500 text-blue-600'
                                        : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                                )}
                                onClick={() => setSelectedTabIndex(index)}
                            >
                                {tab.name}
                            </span>
                        ))}
                    </nav>

                    {tabs[selectedTabIndex].content}
                </div>
            )}
        </span>
    );
};

export default InstanceDialogWorkflowListItem;
