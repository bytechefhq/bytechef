import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import TextArea from 'components/TextArea/TextArea';
import {useGetComponentDefinitionQuery} from 'queries/componentDefinitions.queries';
import {useState} from 'react';

import Select from '../../../../components/Select/Select';
import {Tooltip} from '../../../../components/Tooltip/Tooltip';
import useNodeDetailsDialogStore from '../stores/useNodeDetailsDialogStore';
import TabButton from './TabButton';

const tabs = [
    {
        label: 'Description',
        name: 'description',
    },
    {
        label: 'Properties',
        name: 'properties',
    },
    {
        label: 'Connection',
        name: 'connection',
    },
    {
        label: 'Output',
        name: 'output',
    },
];

const NodeDetailsDialog = () => {
    const [activeTab, setActiveTab] = useState('description');

    const {currentNode, nodeDetailsOpen, setNodeDetailsOpen} =
        useNodeDetailsDialogStore();

    const {data: currentComponent} = useGetComponentDefinitionQuery({
        componentName: currentNode.name,
        componentVersion: currentNode.version,
    });

    return (
        <Dialog.Root
            open={nodeDetailsOpen}
            onOpenChange={() => setNodeDetailsOpen(!nodeDetailsOpen)}
            modal={false}
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-0 right-0 z-10 w-screen max-w-md overflow-hidden border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                >
                    {currentComponent ? (
                        <div className="flex h-full flex-col divide-y divide-gray-100 bg-white shadow-xl">
                            <Dialog.Title className="flex content-center items-center p-4 text-lg font-medium text-gray-900">
                                {currentNode.label}

                                <span className="mx-2 text-sm text-gray-500">
                                    ({currentNode.name})
                                </span>

                                {currentComponent?.display.description && (
                                    <Tooltip
                                        text={
                                            currentComponent?.display
                                                .description
                                        }
                                    >
                                        <InfoCircledIcon className="h-4 w-4" />
                                    </Tooltip>
                                )}

                                <Button
                                    aria-label="Close panel"
                                    className="ml-auto"
                                    displayType="icon"
                                    icon={
                                        <Cross1Icon
                                            className="h-3 w-3 cursor-pointer text-gray-900"
                                            aria-hidden="true"
                                        />
                                    }
                                    onClick={() => setNodeDetailsOpen(false)}
                                />
                            </Dialog.Title>

                            <div className="space-y-4 p-4">
                                <Select
                                    label="Actions"
                                    options={currentComponent?.actions.map(
                                        (action) => ({
                                            label: action.name!,
                                            value: action.display.label!,
                                            description:
                                                action.display.description,
                                        })
                                    )}
                                    triggerClassName="w-full bg-gray-100"
                                />

                                <div className="flex justify-center space-x-1">
                                    {tabs.map((tab) => (
                                        <TabButton
                                            activeTab={activeTab}
                                            handleClick={() =>
                                                setActiveTab(tab.name)
                                            }
                                            key={tab.name}
                                            label={tab.label}
                                            name={tab.name}
                                        />
                                    ))}
                                </div>

                                {activeTab === 'description' && (
                                    <TextArea
                                        label="Node Description"
                                        labelClassName="Node Description"
                                        name="nodeDescription"
                                        placeholder="Write some notes for yourself..."
                                    />
                                )}

                                {activeTab === 'properties' && (
                                    <h1>Properties</h1>
                                )}

                                {activeTab === 'connection' && (
                                    <h1>Connection</h1>
                                )}

                                {activeTab === 'output' && <h1>Output</h1>}
                            </div>

                            <div className="mt-auto flex p-4">
                                <Select
                                    defaultValue={currentComponent?.version.toString()}
                                    options={[
                                        {label: 'v 1', value: '1'},
                                        {label: 'v 2', value: '2'},
                                        {label: 'v 3', value: '3'},
                                    ]}
                                />
                            </div>
                        </div>
                    ) : (
                        <div className="flex w-full justify-center p-4">
                            <span className="text-gray-500">
                                Something went wrong 👾
                            </span>
                        </div>
                    )}
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default NodeDetailsDialog;
