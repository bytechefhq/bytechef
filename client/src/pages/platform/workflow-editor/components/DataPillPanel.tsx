import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import DataPillPanelBody, {ComponentOperationType} from '@/pages/platform/workflow-editor/components/DataPillPanelBody';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {useEffect, useState} from 'react';

import useDataPillPanelStore from '../stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

const DataPillPanel = ({
    previousComponentDefinitions,
    workflowNodeOutputs,
}: {
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    workflowNodeOutputs: Array<WorkflowNodeOutput>;
}) => {
    const [dataPillFilterQuery, setDataPillFilterQuery] = useState('');

    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();
    const {workflow} = useWorkflowDataStore();
    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const validWorkflowNodeOutputs = workflowNodeOutputs.filter((workflowNodeOutput) => {
        const {actionDefinition, triggerDefinition, workflowNodeName} = workflowNodeOutput;

        return (
            workflowNodeName !== currentNode?.name &&
            (actionDefinition?.outputDefined || triggerDefinition?.outputDefined)
        );
    });

    const componentOperations = validWorkflowNodeOutputs.map((workflowNodeOutput) => {
        const {actionDefinition, triggerDefinition} = workflowNodeOutput;

        const componentDefinition = previousComponentDefinitions?.find(
            (currentComponentDefinition) =>
                currentComponentDefinition.name === actionDefinition?.componentName ||
                currentComponentDefinition.name === triggerDefinition?.componentName
        );

        return {
            ...actionDefinition,
            componentDefinition,
            outputSchema: workflowNodeOutput.outputSchema,
            sampleOutput: workflowNodeOutput.sampleOutput,
            workflowNodeName: workflowNodeOutput.workflowNodeName,
        } as ComponentOperationType;
    });

    useEffect(() => {
        if (!workflowNodeDetailsPanelOpen) {
            setDataPillPanelOpen(false);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeDetailsPanelOpen]);

    if (!dataPillPanelOpen) {
        return <></>;
    }

    return (
        <div className="absolute inset-y-4 right-data-pill-panel-placement z-10 w-screen max-w-data-pill-panel-width overflow-hidden rounded-xl border border-border/50 bg-white shadow-lg">
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                <header className="flex content-center items-center p-4 text-lg font-medium">
                    <span>Data Pill Panel</span>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <InfoCircledIcon className="ml-1 size-4" />
                        </TooltipTrigger>

                        <TooltipContent>
                            To use data from the previous step drag its data pill into a field, or click on the data
                            pill.
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
                    <div className="mb-0 border-b border-b-border/50 p-4">
                        <Input
                            name="dataPillFilter"
                            onChange={(event) => setDataPillFilterQuery(event.target.value)}
                            placeholder="Filter Data Pills..."
                            value={dataPillFilterQuery}
                        />
                    </div>

                    {(componentOperations && !!componentOperations.length) ||
                    (workflow.inputs && !!workflow.inputs.length) ? (
                        <DataPillPanelBody
                            componentOperations={componentOperations}
                            dataPillFilterQuery={dataPillFilterQuery}
                        />
                    ) : (
                        <span className="p-4 text-sm text-muted-foreground">No available data pills.</span>
                    )}
                </main>
            </div>
        </div>
    );
};

export default DataPillPanel;
