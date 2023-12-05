import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ActionDefinitionModel, ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import DataPillPanelBody, {ComponentActionData} from '@/pages/automation/project/components/DataPillPanelBody';
import {PropertyType} from '@/types/projectTypes';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import Input from 'components/Input/Input';
import {useState} from 'react';

import {useDataPillPanelStore} from '../stores/useDataPillPanelStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';

const DataPillPanel = ({
    actionData,
    normalizedPreviousComponentNames,
    previousComponentDefinitions,
    previousComponentNames,
}: {
    actionData: Array<ActionDefinitionModel>;
    normalizedPreviousComponentNames: Array<string>;
    previousComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    previousComponentNames: Array<string>;
}) => {
    const [dataPillFilterQuery, setDataPillFilterQuery] = useState('');

    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();
    const {componentNames} = useWorkflowDataStore();

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const actionDataWithComponentAlias = actionData?.map((action) => {
        const sameNameActions = actionData?.filter((actionDatum) => actionDatum.componentName === action.componentName);

        const sameNameIndex = sameNameActions.indexOf(action);

        return {
            ...action,
            workflowAlias: `${action.componentName}-${sameNameIndex + 1}`,
        };
    });

    const previousActions = actionDataWithComponentAlias?.filter((action) =>
        previousComponentNames.includes(action.workflowAlias!)
    );

    const componentActionData = previousActions?.map((action, index) => {
        const componentDefinition = previousComponentDefinitions?.find(
            (currentComponentDefinition) => currentComponentDefinition.name === normalizedPreviousComponentNames[index]
        );

        if (previousComponentNames.includes(action.workflowAlias!)) {
            return {
                ...action,
                componentDefinition,
                workflowAlias: componentNames[index],
            };
        }
    });

    const dataPillComponentData = componentActionData?.filter((action) => {
        if (!action) {
            return false;
        }

        const outputSchemaContent =
            (action.outputSchema as PropertyType)?.properties || (action.outputSchema as PropertyType)?.items;

        return action.workflowAlias !== currentNode.name && action.componentDefinition && outputSchemaContent;
    });

    return (
        <Dialog.Root
            modal={false}
            onOpenChange={() => setDataPillPanelOpen(!dataPillPanelOpen)}
            open={
                dataPillPanelOpen &&
                workflowNodeDetailsPanelOpen &&
                !!previousComponentNames.length &&
                !!previousComponentDefinitions &&
                !!dataPillComponentData.length
            }
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-2 bottom-4 right-[530px] top-[70px] z-10 w-screen max-w-[400px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                    onOpenAutoFocus={(event) => event.preventDefault()}
                >
                    <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                        <Dialog.Title className="flex content-center items-center p-4 text-lg font-medium">
                            <span>Data Pill Panel</span>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoCircledIcon className="ml-1 h-4 w-4" />
                                </TooltipTrigger>

                                <TooltipContent>
                                    To use data from the previous step drag its data pill into a field, or click on the
                                    data pill.
                                </TooltipContent>
                            </Tooltip>

                            <Button
                                aria-label="Close the data pill panel"
                                className="ml-auto pr-0"
                                onClick={() => setDataPillPanelOpen(false)}
                                size="icon"
                                variant="ghost"
                            >
                                <Cross1Icon aria-hidden="true" className="h-3 w-3 cursor-pointer" />
                            </Button>
                        </Dialog.Title>

                        <div className="flex w-full grow flex-col">
                            <Input
                                fieldsetClassName="p-4 border-b border-gray-100 mb-0"
                                name="dataPillFilter"
                                onChange={(event) => setDataPillFilterQuery(event.target.value)}
                                placeholder="Filter Data Pills..."
                                value={dataPillFilterQuery}
                            />

                            {dataPillComponentData && (
                                <DataPillPanelBody
                                    componentData={dataPillComponentData as Array<ComponentActionData>}
                                    dataPillFilterQuery={dataPillFilterQuery}
                                />
                            )}
                        </div>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default DataPillPanel;
