import {ScrollArea} from '@/components/ui/scroll-area';
import DataPillPanelBodyInputsItem from '@/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyInputsItem';
import {
    ActionDefinition,
    ComponentDefinitionBasic,
    type Property,
    TaskDispatcherDefinitionBasic,
    TriggerDefinition,
    WorkflowInput,
} from '@/shared/middleware/platform/configuration';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';
import {InfoIcon} from 'lucide-react';

import DataPillPanelBodyPropertiesItem from './DataPillPanelBodyPropertiesItem';

interface BaseComponentOperationI {
    workflowNodeName: string;
    outputSchema: Property;
    sampleOutput: object;
}

interface ComponentActionOperationI extends BaseComponentOperationI {
    componentDefinition: ComponentDefinitionBasic;
    actionDefinition: ActionDefinition;
    taskDispatcherDefinition: never;
}

interface ComponentTriggerOperationI extends BaseComponentOperationI {
    componentDefinition: ComponentDefinitionBasic;
    taskDispatcherDefinition: never;
    triggerDefinition: TriggerDefinition;
}

interface TaskDispatcherOperationI extends BaseComponentOperationI {
    componentDefinition: never;
    taskDispatcherDefinition: TaskDispatcherDefinitionBasic;
}

export type OperationType = ComponentActionOperationI | ComponentTriggerOperationI | TaskDispatcherOperationI;

interface DataPillPanelBodyProps {
    operations: Array<OperationType>;
    dataPillFilterQuery: string;
    workflowInputs?: WorkflowInput[];
}

const DataPillPanelBody = ({dataPillFilterQuery, operations, workflowInputs}: DataPillPanelBodyProps) => {
    const hasWorkflowInputs = !!workflowInputs?.length;
    const hasOperations = operations.length > 0;

    if (!hasWorkflowInputs && !hasOperations) {
        return (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center">
                <InfoIcon className="size-8 text-muted-foreground/50" />

                <div className="space-y-1">
                    <p className="text-sm font-medium text-muted-foreground">No data pills available</p>

                    <p className="text-xs text-muted-foreground/70">
                        Add previous steps to the workflow to use their output data here.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <ScrollArea className="h-full w-full">
            <Accordion className="size-full max-w-data-pill-panel-width" collapsible type="single">
                {hasWorkflowInputs && (
                    <AccordionItem className="group" value="inputs">
                        <DataPillPanelBodyInputsItem />
                    </AccordionItem>
                )}

                {operations.map((operation) => (
                    <AccordionItem
                        className="group"
                        key={`accordion-item-${operation.workflowNodeName}`}
                        value={operation.workflowNodeName}
                    >
                        <DataPillPanelBodyPropertiesItem
                            dataPillFilterQuery={dataPillFilterQuery}
                            operation={operation}
                            sampleOutput={operation.sampleOutput}
                        />
                    </AccordionItem>
                ))}
            </Accordion>
        </ScrollArea>
    );
};

export default DataPillPanelBody;
