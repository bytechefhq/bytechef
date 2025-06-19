import {Input} from '@/components/ui/input';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import DataPillPanelBody, {
    OperationType,
} from '@/pages/platform/workflow-editor/components/datapills/DataPillPanelBody';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {Cross2Icon, InfoCircledIcon} from '@radix-ui/react-icons';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useDataPillPanelStore from '../../stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';

const LoadingSkeleton = () => (
    <ul className="flex flex-col">
        <li className="flex items-center space-x-4 border-b border-border/50 p-4">
            <Skeleton className="size-6" />

            <Skeleton className="h-6 w-2/3" />

            <Skeleton className="h-6 w-1/5" />
        </li>

        <li className="flex items-center space-x-4 border-b border-border/50 p-4">
            <Skeleton className="size-6" />

            <Skeleton className="h-6 w-2/3" />

            <Skeleton className="h-6 w-1/5" />
        </li>

        <li className="flex items-center space-x-4 border-b border-border/50 p-4">
            <Skeleton className="size-6" />

            <Skeleton className="h-6 w-2/3" />

            <Skeleton className="h-6 w-1/5" />
        </li>

        <li className="flex items-center space-x-4 border-b border-border/50 p-4">
            <Skeleton className="size-6" />

            <Skeleton className="h-6 w-2/3" />

            <Skeleton className="h-6 w-1/5" />
        </li>
    </ul>
);

interface DataPillPanelProps {
    className?: string;
    isLoading: boolean;
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    workflowNodeOutputs: Array<WorkflowNodeOutput>;
}

const DataPillPanel = ({
    className,
    isLoading,
    previousComponentDefinitions,
    workflowNodeOutputs,
}: DataPillPanelProps) => {
    const [dataPillFilterQuery, setDataPillFilterQuery] = useState('');

    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();
    const {workflow} = useWorkflowDataStore();
    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

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

    if (!dataPillPanelOpen) {
        return <></>;
    }

    const hasAvailableDataPills =
        (operations && operations.length > 0) || (workflow.inputs && workflow.inputs.length > 0);

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

                <main className="flex w-full grow flex-col overflow-hidden">
                    <div className="mb-0 border-b border-b-border/50 p-4">
                        <Input
                            name="dataPillFilter"
                            onChange={(event) => setDataPillFilterQuery(event.target.value)}
                            placeholder="Filter Data Pills..."
                            value={dataPillFilterQuery}
                        />
                    </div>

                    {(!isLoading || currentNode?.trigger) && !hasAvailableDataPills && (
                        <span className="p-4 text-sm text-muted-foreground">No available data pills.</span>
                    )}

                    {!currentNode?.trigger && isLoading && <LoadingSkeleton />}

                    <DataPillPanelBody
                        dataPillFilterQuery={dataPillFilterQuery}
                        operations={operations}
                        workflowInputs={workflow.inputs}
                    />
                </main>
            </div>
        </div>
    );
};

export default DataPillPanel;
