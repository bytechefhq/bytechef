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

    const singleActionComponent = currentComponent?.actions.length === 1;

    const firstAction = currentComponent?.actions[0];

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

                            <div className="flex flex-col">
                                <div className="p-4">
                                    {singleActionComponent && !!firstAction ? (
                                        <>
                                            <span className="block px-2 text-sm font-medium leading-6">
                                                Action
                                            </span>

                                            <div className="overflow-hidden rounded-md bg-gray-100 py-2">
                                                <span className="inline-flex px-4 text-sm font-medium">
                                                    {firstAction.display.label}
                                                </span>

                                                {/* eslint-disable-next-line tailwindcss/no-custom-classname */}

                                                <p className="mt-1 w-full overflow-hidden px-4 text-xs text-gray-500 line-clamp-2">
                                                    {
                                                        firstAction.display
                                                            .description
                                                    }
                                                </p>
                                            </div>
                                        </>
                                    ) : (
                                        <Select
                                            contentClassName="max-w-select-trigger-width max-h-select-content-available-height-1/2"
                                            label="Actions"
                                            options={currentComponent?.actions.map(
                                                (action) => ({
                                                    label: action.display
                                                        .label!,
                                                    value: action.name,
                                                    description:
                                                        action.display
                                                            .description,
                                                })
                                            )}
                                            triggerClassName="w-full bg-gray-100"
                                        />
                                    )}
                                </div>

                                <div className="border-t border-gray-100" />

                                <div className="flex justify-center space-x-1 p-4">
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

                                <div className="px-4 py-2">
                                    {activeTab === 'description' && (
                                        <TextArea
                                            label="Description"
                                            labelClassName="px-2"
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
                            </div>

                            <div className="mt-auto flex p-4">
                                <Select
                                    defaultValue={currentComponent?.version.toString()}
                                    options={[
                                        {label: 'v1', value: '1'},
                                        {label: 'v2', value: '2'},
                                        {label: 'v3', value: '3'},
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
