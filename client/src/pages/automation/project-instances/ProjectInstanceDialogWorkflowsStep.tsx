import {Switch} from '@/components/ui/switch';
import Button from 'components/Button/Button';
import Input from 'components/Input/Input';
import Properties from 'components/Properties/Properties';
import {
    ProjectInstanceModel,
    WorkflowConnectionModel,
    WorkflowModel,
} from 'middleware/helios/configuration';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {useState} from 'react';
import {UseFormGetValues} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

interface ProjectInstanceDialogWorkflowListItemProps {
    workflow: WorkflowModel;
    label: string;
}

export const ProjectInstanceDialogWorkflowListItem = ({
    label,
    workflow,
}: ProjectInstanceDialogWorkflowListItemProps) => {
    const [selectedTabIndex, setSelectedTabIndex] = useState(0);
    const [isEnabled, setIsEnabled] = useState(false);

    const connections: WorkflowConnectionModel[] = [];

    workflow.tasks?.forEach(
        (task) => task.connections && connections.concat(task.connections)
    );
    workflow.triggers?.forEach(
        (trigger) =>
            trigger.connections && connections.concat(trigger.connections)
    );

    const tabs = [
        {
            content: workflow.inputs && (
                <Properties
                    properties={workflow.inputs.map((input) => {
                        if (input.type === 'string') {
                            return {
                                ...input,
                                controlType: 'TEXT',
                                type: 'STRING',
                            };
                        } else if (input.type === 'number') {
                            return {
                                ...input,
                                type: 'NUMBER',
                            };
                        } else {
                            return {
                                ...input,
                                controlType: 'SELECT',
                                type: 'BOOLEAN',
                            };
                        }
                    })}
                />
            ),
            name: 'Configuration',
        },
        {
            content: (
                <>
                    {connections?.map((connection) => (
                        <Input
                            key={connection.componentName}
                            label={connection.componentName}
                            labelClassName="px-2"
                            name={connection.componentName}
                        />
                    ))}
                </>
            ),
            name: 'Connections',
        },
    ];

    return (
        <div>
            <div
                className="flex cursor-pointer justify-between py-2"
                onClick={() => setIsEnabled(!isEnabled)}
            >
                <span className="font-semibold">{label}</span>

                <Switch
                    checked={isEnabled}
                    className={twMerge(
                        'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                        isEnabled && 'bg-blue-600'
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
                <div className="mt-2">
                    {connections?.length ? (
                        <>
                            <nav aria-label="Tabs" className="flex">
                                {tabs.map((tab, index) => (
                                    <Button
                                        key={tab.name}
                                        label={tab.name}
                                        className={twMerge(
                                            'grow justify-center whitespace-nowrap rounded-none border-0 border-b-2 border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                            selectedTabIndex === index &&
                                                'border-blue-500 text-blue-500 hover:text-blue-500'
                                        )}
                                        onClick={() =>
                                            setSelectedTabIndex(index)
                                        }
                                    />
                                ))}
                            </nav>

                            {tabs[selectedTabIndex].content}
                        </>
                    ) : (
                        <>{tabs[0].content}</>
                    )}
                </div>
            )}
        </div>
    );
};

const ProjectInstanceDialogWorkflowsStep = (props: {
    getValues: UseFormGetValues<ProjectInstanceModel>;
}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(
        props.getValues().projectId!
    );

    return (
        <ul className="space-y-4">
            {workflows?.map((workflow) => (
                <ProjectInstanceDialogWorkflowListItem
                    key={workflow.id!}
                    workflow={workflow}
                    label={workflow.label!}
                />
            ))}
        </ul>
    );
};

export default ProjectInstanceDialogWorkflowsStep;
