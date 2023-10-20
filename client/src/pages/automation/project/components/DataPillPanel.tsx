import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {
    ActionDefinitionModel,
    ComponentDefinitionBasicModel,
} from '@/middleware/hermes/configuration';
import {useGetActionDefinitionsQuery} from '@/queries/actionDefinitions.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import Input from 'components/Input/Input';
import {useCallback, useState} from 'react';

import {useDataPillPanelStore} from '../stores/useDataPillPanelStore';
import {useNodeDetailsDialogStore} from '../stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import DataPillPanelBody from './DataPillPanelBody';

export type ComponentActionData = {
    component: ComponentDefinitionBasicModel;
    workflowAlias: string;
} & ActionDefinitionModel;

const DataPillPanel = () => {
    const [panelContainerHeight, setPanelContainerHeight] = useState(0);
    const [dataPillFilterQuery, setDataPillFilterQuery] = useState('');

    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();
    const {currentNode, nodeDetailsDialogOpen} = useNodeDetailsDialogStore();
    const {componentNames} = useWorkflowDefinitionStore();

    const panelContainerRef = useCallback(
        (panelContainer: HTMLDivElement) =>
            setPanelContainerHeight(
                panelContainer?.getBoundingClientRect().height
            ),
        []
    );

    const currentNodeIndex = componentNames.indexOf(currentNode.name);

    const previousComponentNames =
        componentNames.length > 1
            ? componentNames.slice(0, currentNodeIndex)
            : [];

    const normalizedPreviousComponentNames = previousComponentNames.map(
        (name) =>
            name.match(new RegExp(/-\d$/))
                ? name.slice(0, name.length - 2)
                : name
    );

    const {data: previousComponents} = useGetComponentDefinitionsQuery(
        {
            include: normalizedPreviousComponentNames,
        },
        !!normalizedPreviousComponentNames.length
    );

    const {componentActions} = useWorkflowDefinitionStore();

    const taskTypes = componentActions?.map(
        (componentAction) =>
            `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const {data: actionData} = useGetActionDefinitionsQuery(
        {taskTypes},
        !!componentActions?.length
    );

    const componentActionData = actionData?.map((actionDatum, index) => {
        const componentData = previousComponents?.find(
            (component) =>
                component.name === normalizedPreviousComponentNames[index]
        );

        return {
            ...actionDatum,
            component: componentData,
            workflowAlias: componentNames[index],
        };
    });

    if (!componentActionData?.length) {
        return <></>;
    }

    return (
        <Dialog.Root
            open={
                nodeDetailsDialogOpen &&
                dataPillPanelOpen &&
                !!previousComponentNames.length &&
                !!previousComponents
            }
            onOpenChange={() => setDataPillPanelOpen(!dataPillPanelOpen)}
            modal={false}
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-2 right-[472px] top-16 z-10 w-screen max-w-[400px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                    onOpenAutoFocus={(event) => event.preventDefault()}
                >
                    <div
                        className="flex h-full flex-col bg-white shadow-xl"
                        ref={panelContainerRef}
                    >
                        <header className="border-b border-gray-100 p-4">
                            <Dialog.Title className="flex content-center items-center text-lg font-medium text-gray-900">
                                <span>Data Pill Panel</span>

                                <TooltipProvider>
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <InfoCircledIcon className="ml-1 h-4 w-4" />
                                        </TooltipTrigger>

                                        <TooltipContent>
                                            To use data from the previous step
                                            drag its datapill into a field, or
                                            click on the datapill.
                                        </TooltipContent>
                                    </Tooltip>
                                </TooltipProvider>

                                <Button
                                    aria-label="Close the data pill panel"
                                    className="ml-auto"
                                    displayType="icon"
                                    icon={
                                        <Cross1Icon
                                            className="h-3 w-3 cursor-pointer text-gray-900"
                                            aria-hidden="true"
                                        />
                                    }
                                    onClick={() => setDataPillPanelOpen(false)}
                                />
                            </Dialog.Title>
                        </header>

                        <main className="flex h-full w-full flex-col">
                            <Input
                                name="dataPillFilter"
                                placeholder="Filter Data Pills..."
                                fieldsetClassName="p-2 border-b border-gray-100 mb-0"
                                onChange={(event) =>
                                    setDataPillFilterQuery(event.target.value)
                                }
                                value={dataPillFilterQuery}
                            />

                            {actionData?.length && (
                                <DataPillPanelBody
                                    actionData={actionData}
                                    containerHeight={panelContainerHeight}
                                    dataPillFilterQuery={dataPillFilterQuery}
                                    previousComponents={previousComponents!}
                                    previousComponentNames={
                                        previousComponentNames
                                    }
                                    componentActionData={
                                        componentActionData.filter(
                                            (data) => data.component
                                        ) as Array<ComponentActionData>
                                    }
                                />
                            )}
                        </main>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default DataPillPanel;
