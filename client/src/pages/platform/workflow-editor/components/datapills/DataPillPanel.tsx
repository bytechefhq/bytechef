import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import DataPillPanelBody, {
    OperationType,
} from '@/pages/platform/workflow-editor/components/datapills/DataPillPanelBody';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {InfoIcon, XIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import useDataPillPanelStore from '../../stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';

interface DataPillPanelProps {
    className?: string;
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    workflowNodeOutputs: Array<WorkflowNodeOutput>;
}

const DataPillPanel = ({className, previousComponentDefinitions, workflowNodeOutputs}: DataPillPanelProps) => {
    const [dataPillFilterQuery, setDataPillFilterQuery] = useState('');

    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );

    const validWorkflowNodeOutputs = workflowNodeOutputs.filter((workflowNodeOutput) => {
        const {actionDefinition, taskDispatcherDefinition, triggerDefinition, workflowNodeName} = workflowNodeOutput;

        if (workflowNodeName === currentNode?.name) {
            return false;
        }

        return (
            actionDefinition?.outputDefined ||
            triggerDefinition?.outputDefined ||
            taskDispatcherDefinition?.outputDefined ||
            taskDispatcherDefinition?.variablePropertiesDefined
        );
    });

    const operations = validWorkflowNodeOutputs.map((workflowNodeOutput) => {
        const {actionDefinition, triggerDefinition} = workflowNodeOutput;

        const componentDefinition = previousComponentDefinitions?.find(
            (currentComponentDefinition) =>
                currentComponentDefinition.name === actionDefinition?.componentName ||
                currentComponentDefinition.name === triggerDefinition?.componentName
        );

        return {
            ...actionDefinition,
            componentDefinition,
            outputSchema:
                workflowNodeOutput.outputResponse?.outputSchema ||
                workflowNodeOutput.variableOutputResponse?.outputSchema,
            sampleOutput:
                workflowNodeOutput.outputResponse?.sampleOutput ||
                workflowNodeOutput.variableOutputResponse?.sampleOutput,
            taskDispatcherDefinition: workflowNodeOutput.taskDispatcherDefinition,
            workflowNodeName: workflowNodeOutput.workflowNodeName,
        } as OperationType;
    });

    useEffect(() => {
        if (!workflowNodeDetailsPanelOpen) {
            setDataPillPanelOpen(false);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeDetailsPanelOpen]);

    return (
        <div
            className={twMerge(
                'absolute bottom-6 right-[536px] top-2 z-10 w-screen max-w-data-pill-panel-width overflow-hidden rounded-md border border-stroke-neutral-secondary bg-background',
                className
            )}
        >
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-white">
                <header className="flex content-center items-center p-4 text-lg font-medium">
                    <span>Data Pill Panel</span>

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <InfoIcon className="ml-1 size-4" />
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
                        <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                    </button>
                </header>

                <main className="flex grow flex-col overflow-hidden">
                    <div className="mb-0 border-b border-b-border/50 p-4">
                        <Input
                            name="dataPillFilter"
                            onChange={(event) => setDataPillFilterQuery(event.target.value)}
                            placeholder="Filter Data Pills..."
                            value={dataPillFilterQuery}
                        />
                    </div>

                    <div className="flex min-h-0 flex-1 overflow-hidden bg-surface-main">
                        <DataPillPanelBody
                            dataPillFilterQuery={dataPillFilterQuery}
                            operations={operations}
                            workflowInputs={workflow.inputs}
                        />
                    </div>
                </main>
            </div>
        </div>
    );
};

export default DataPillPanel;
