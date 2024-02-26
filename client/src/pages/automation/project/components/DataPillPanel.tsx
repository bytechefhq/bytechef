import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel, WorkflowNodeOutputModel} from '@/middleware/platform/configuration';
import DataPillPanelBody, {ComponentActionDataI} from '@/pages/automation/project/components/DataPillPanelBody';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {useState} from 'react';

import {useDataPillPanelStore} from '../stores/useDataPillPanelStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';

const DataPillPanel = ({
    previousComponentDefinitions,
    workflowNodeOutputs,
}: {
    previousComponentDefinitions: Array<ComponentDefinitionBasicModel>;
    workflowNodeOutputs: Array<WorkflowNodeOutputModel>;
}) => {
    const [dataPillFilterQuery, setDataPillFilterQuery] = useState('');

    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const componentActionData: Array<ComponentActionDataI> = workflowNodeOutputs
        .filter((workflowStepOutput) => workflowStepOutput?.actionDefinition)
        .filter(
            (workflowStepOutput) =>
                workflowStepOutput.workflowNodeName !== currentNode.name &&
                (workflowStepOutput.actionDefinition!.outputDefined ||
                    workflowStepOutput.actionDefinition!.outputFunctionDefined)
        )
        .map((workflowStepOutput) => {
            return {
                ...workflowStepOutput.actionDefinition,
                componentDefinition: previousComponentDefinitions?.find(
                    (currentComponentDefinition) =>
                        currentComponentDefinition.name === workflowStepOutput.actionDefinition!.componentName
                ),
                outputSchema: workflowStepOutput.outputSchema,
                sampleOutput: workflowStepOutput.sampleOutput,
                workflowNodeName: workflowStepOutput.workflowNodeName,
            } as ComponentActionDataI;
        });

    return (
        <Dialog.Root
            modal={false}
            onOpenChange={() => setDataPillPanelOpen(!dataPillPanelOpen)}
            open={
                dataPillPanelOpen &&
                workflowNodeDetailsPanelOpen &&
                !!previousComponentDefinitions &&
                !!componentActionData.length
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
                                    <InfoCircledIcon className="ml-1 size-4" />
                                </TooltipTrigger>

                                <TooltipContent>
                                    To use data from the previous step drag its data pill into a field, or click on the
                                    data pill.
                                </TooltipContent>
                            </Tooltip>

                            <button
                                aria-label="Close the data pill panel"
                                className="ml-auto pr-0"
                                onClick={() => setDataPillPanelOpen(false)}
                            >
                                <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                            </button>
                        </Dialog.Title>

                        <div className="flex w-full grow flex-col">
                            <div className="mb-0 border-b border-gray-100 p-4">
                                <Input
                                    name="dataPillFilter"
                                    onChange={(event) => setDataPillFilterQuery(event.target.value)}
                                    placeholder="Filter Data Pills..."
                                    value={dataPillFilterQuery}
                                />
                            </div>

                            {componentActionData && (
                                <DataPillPanelBody
                                    componentActionData={componentActionData}
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
