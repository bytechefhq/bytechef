import {Switch} from '@headlessui/react';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {WorkflowModel} from 'middleware/automation/project';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

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
            <Input label="Property 1" labelClassName="px-2" name="property1" />

            <Input label="Property 2" labelClassName="px-2" name="property2" />

            <TextArea
                label="Property 3"
                labelClassName="px-2"
                name="property3"
            />
        </div>
    );

    const ConnectionForm = () => (
        <div>
            <Input label="Pipedrive" labelClassName="px-2" name="pipedrive" />

            <Input label="Mailchimp" labelClassName="px-2" name="mailchimp" />
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
        <div className="mb-4">
            <div className="flex justify-items-end ">
                <span className="pr-20 font-semibold">
                    {workflowModels.label}
                </span>

                <Switch
                    checked={isEnabled}
                    onChange={toggleIsEnabled}
                    className={twMerge(
                        'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                        isEnabled ? 'bg-indigo-600' : 'bg-gray-200'
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
            </div>

            {isEnabled && (
                <div className="hidden border-b border-gray-200 sm:block">
                    <nav className="flex space-x-8" aria-label="Tabs">
                        {tabs.map((tab, index) => (
                            <span
                                key={tab.name}
                                className={twMerge(
                                    'cursor-pointer whitespace-nowrap border-b-2 px-1 py-4 text-sm font-medium',
                                    selectedTabIndex === index
                                        ? 'border-indigo-500 text-indigo-600'
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
        </div>
    );
};

export default InstanceDialogWorkflowListItem;
