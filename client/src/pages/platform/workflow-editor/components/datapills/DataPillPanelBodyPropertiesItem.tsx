import {Button} from '@/components/ui/button';
import DataPill from '@/pages/platform/workflow-editor/components/datapills/DataPill';
import getNestedObject from '@/pages/platform/workflow-editor/utils/getNestedObject';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon} from 'lucide-react';
import {Fragment} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import useNodeClickHandler from '../../hooks/useNodeClick';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {OperationType} from './DataPillPanelBody';

function getFilteredProperties({filterQuery, properties}: {filterQuery: string; properties?: Array<PropertyAllType>}) {
    if (!properties || !properties.length) {
        return [];
    }

    return properties.reduce((previousValue: Array<PropertyAllType>, currentValue) => {
        const subProperties = getFilteredProperties({
            filterQuery,
            properties: currentValue.properties || currentValue.items || [],
        });

        if (currentValue.name?.toLowerCase().includes(filterQuery.toLowerCase()) || subProperties.length) {
            previousValue.push(Object.assign({}, currentValue));
        }

        return previousValue;
    }, []);
}

interface DataPillPanelBodyPropertiesItemProps {
    operation: OperationType;
    dataPillFilterQuery: string;
    sampleOutput: string | number | boolean | object;
}

const DataPillPanelBodyPropertiesItem = ({
    dataPillFilterQuery,
    operation,
    sampleOutput,
}: DataPillPanelBodyPropertiesItemProps) => {
    const {componentDefinitions, workflowNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            workflowNodes: state.workflowNodes,
        }))
    );
    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const {workflowNodeName} = operation;

    const outputSchema: PropertyAllType | undefined = operation?.outputSchema;

    const properties: Array<PropertyAllType> | undefined = outputSchema?.properties || outputSchema?.items;

    const filteredProperties = getFilteredProperties({
        filterQuery: dataPillFilterQuery,
        properties,
    });

    let icon: string | undefined, title: string | undefined;

    if (operation.componentDefinition) {
        icon = operation.componentDefinition.icon;
        title = operation.componentDefinition.title;
    }

    if (operation.taskDispatcherDefinition) {
        icon = operation.taskDispatcherDefinition.icon;
        title = operation.taskDispatcherDefinition.title;
    }

    const currentWorkflowNode = workflowNodes.find(
        (workflowNode) => workflowNode.workflowNodeName === workflowNodeName
    );

    const currentComponentDefinition = componentDefinitions.find(
        (componentDefinition) => componentDefinition.name === currentWorkflowNode?.name
    );

    const redirectTargetNode = nodes.find((workflowNode) => workflowNode.id === workflowNodeName);

    const handleOutputTabRedirectClick = useNodeClickHandler(
        redirectTargetNode?.data as NodeDataType,
        redirectTargetNode?.data.name as string,
        'output'
    );

    return (
        <Fragment key={`accordion-item-${workflowNodeName}`}>
            <AccordionTrigger
                className="group flex w-full items-center justify-between border-border/50 bg-surface-main p-4 group-data-[state=closed]:border-b"
                key={`accordion-trigger-${workflowNodeName}`}
            >
                <div className="flex items-center space-x-4">
                    {icon && (
                        <div className="flex items-center">
                            <InlineSVG className="size-5" src={icon} />
                        </div>
                    )}

                    <h3 className="flex flex-col items-start text-sm">
                        {title}

                        <span className="truncate text-xs text-gray-400">({workflowNodeName})</span>
                    </h3>
                </div>

                {currentWorkflowNode?.operationName && (
                    <span className="ml-auto mr-4 max-w-36 truncate rounded bg-muted px-2 py-1 text-xs">
                        {currentWorkflowNode?.operationName}
                    </span>
                )}

                <ChevronDownIcon className="size-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
            </AccordionTrigger>

            <AccordionContent
                className="size-full space-y-2 border-b border-b-border/50 px-4 pb-4"
                key={`accordion-content-${workflowNodeName}`}
            >
                {outputSchema ? (
                    <>
                        <DataPill
                            componentIcon={icon}
                            property={outputSchema}
                            root={true}
                            sampleOutput={sampleOutput}
                            workflowNodeName={workflowNodeName}
                        />

                        <ul className="flex w-full flex-col space-y-2 border-l border-l-border/50 pl-4 group-data-[state=open]:h-full">
                            {filteredProperties?.map((property, index) => {
                                let value;

                                if (typeof sampleOutput === 'object' && property.name) {
                                    value = getNestedObject(sampleOutput, property.name.replaceAll('/', '.'));
                                } else {
                                    value = sampleOutput;
                                }

                                return (
                                    <div
                                        className="flex items-center space-x-3"
                                        key={`${property.name}-${workflowNodeName}-${index}`}
                                    >
                                        <DataPill
                                            componentIcon={icon}
                                            property={property}
                                            sampleOutput={sampleOutput}
                                            workflowNodeName={workflowNodeName}
                                        />

                                        {value !== undefined && typeof value !== 'object' && (
                                            <div className="flex-1 overflow-hidden truncate text-xs text-muted-foreground">
                                                {String(value)}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </ul>
                    </>
                ) : (
                    <div className="flex flex-col gap-3">
                        {currentComponentDefinition ? (
                            <>
                                <span className="font-semibold">Test Action</span>

                                <p className="text-sm">
                                    <span className="font-semibold">{currentWorkflowNode?.workflowNodeName} </span>
                                    needs to be tested to generate an output schema. Please go to the
                                    <span className="font-semibold"> &quot;Output&quot; </span>
                                    tab of the
                                    <span className="font-semibold"> {currentWorkflowNode?.workflowNodeName} </span>
                                    action.
                                </p>
                            </>
                        ) : (
                            <>
                                <span className="font-semibold">Configure Flow</span>

                                <p className="text-sm">
                                    <span className="font-semibold">{currentWorkflowNode?.workflowNodeName} </span>
                                    needs to be configured. Please go to the
                                    <span className="font-semibold"> &quot;Properties&quot; </span>
                                    tab of the
                                    <span className="font-semibold"> {currentWorkflowNode?.workflowNodeName} </span>
                                    flow.
                                </p>
                            </>
                        )}

                        <Button
                            className="hover:bg-surface-brand-secondary-hover"
                            onClick={handleOutputTabRedirectClick}
                            variant="secondary"
                        >
                            Go to
                            <span className="-ml-1 font-semibold">{currentWorkflowNode?.workflowNodeName}</span>
                        </Button>
                    </div>
                )}
            </AccordionContent>
        </Fragment>
    );
};

export default DataPillPanelBodyPropertiesItem;
