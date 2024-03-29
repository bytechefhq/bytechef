import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel, WorkflowNodeOutputModel} from '@/middleware/platform/configuration';
import DataPillPanelBody, {ComponentOperationType} from '@/pages/automation/project/components/DataPillPanelBody';
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

    const componentOperations: Array<ComponentOperationType> = workflowNodeOutputs
        .filter(
            (workflowNodeOutput) =>
                workflowNodeOutput.workflowNodeName !== currentNode.name &&
                (workflowNodeOutput.actionDefinition?.outputDefined ||
                    workflowNodeOutput.actionDefinition?.outputFunctionDefined ||
                    workflowNodeOutput.triggerDefinition?.outputDefined ||
                    workflowNodeOutput.triggerDefinition?.outputFunctionDefined)
        )
        .map((workflowNodeOutput) => {
            return {
                ...workflowNodeOutput.actionDefinition,
                componentDefinition: previousComponentDefinitions?.find(
                    (currentComponentDefinition) =>
                        currentComponentDefinition.name === workflowNodeOutput.actionDefinition?.componentName ||
                        currentComponentDefinition.name === workflowNodeOutput.triggerDefinition?.componentName
                ),
                outputSchema: workflowNodeOutput.outputSchema,
                sampleOutput: workflowNodeOutput.sampleOutput,
                workflowNodeName: workflowNodeOutput.workflowNodeName,
            } as ComponentOperationType;
        });

    return (
        <>
            {dataPillPanelOpen && workflowNodeDetailsPanelOpen && (
                <div className="absolute inset-y-4 right-[485px] z-10 w-screen max-w-[400px] overflow-hidden rounded-xl border-l bg-white shadow-lg">
                    <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                        <header className="flex content-center items-center p-4 text-lg font-medium">
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
                        </header>

                        <main className="flex w-full grow flex-col">
                            <div className="mb-0 border-b border-gray-100 p-4">
                                <Input
                                    name="dataPillFilter"
                                    onChange={(event) => setDataPillFilterQuery(event.target.value)}
                                    placeholder="Filter Data Pills..."
                                    value={dataPillFilterQuery}
                                />
                            </div>

                            {componentOperations && (
                                <DataPillPanelBody
                                    componentOperations={componentOperations}
                                    dataPillFilterQuery={dataPillFilterQuery}
                                />
                            )}
                        </main>
                    </div>
                </div>
            )}
        </>
    );
};

export default DataPillPanel;
