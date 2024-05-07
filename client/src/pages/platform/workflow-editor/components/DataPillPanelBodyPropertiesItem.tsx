import {Button} from '@/components/ui/button';
import DataPill from '@/pages/platform/workflow-editor/components/DataPill';
import getNestedObject from '@/pages/platform/workflow-editor/utils/getNestedObject';
import {PropertyType} from '@/types/types';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {useReactFlow} from 'reactflow';

import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {ComponentActionI} from './DataPillPanelBody';

const DataPillPanelBodyPropertiesItem = ({
    componentAction,
    filteredProperties,
    outputSchemaExists,
    sampleOutput,
}: {
    componentAction: ComponentActionI;
    filteredProperties: Array<PropertyType>;
    outputSchemaExists: boolean;
    sampleOutput: object;
}) => {
    const {icon, title} = componentAction.componentDefinition;

    const {componentActions} = useWorkflowDataStore();
    const {getNodes} = useReactFlow();

    const currentComponentAction = componentActions.find(
        (action) => action.workflowNodeName === componentAction.workflowNodeName
    );

    const nodes = getNodes();

    const redirectTargetNode = nodes.find((workflowNode) => workflowNode.id === componentAction.workflowNodeName);

    const handleOutputTabRedirectClick = useNodeClickHandler(redirectTargetNode?.data, redirectTargetNode?.data.name);

    return (
        <>
            <AccordionTrigger
                className="group flex w-full items-center justify-between border-gray-100 bg-white p-4 group-data-[state=closed]:border-b"
                key={`accordion-trigger-${componentAction.workflowNodeName}`}
            >
                <div className="flex items-center space-x-4">
                    {icon && (
                        <div className="flex size-5 items-center">
                            <InlineSVG src={icon} />
                        </div>
                    )}

                    <span className="text-sm">
                        {title}

                        <span className="pl-1 text-xs text-gray-400">({componentAction.workflowNodeName})</span>
                    </span>
                </div>

                <span className="ml-auto mr-4 rounded bg-muted px-2 py-1 text-xs">
                    {currentComponentAction?.operationName}
                </span>

                <ChevronDownIcon className="size-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
            </AccordionTrigger>

            <AccordionContent
                className="size-full space-y-2 border-b border-gray-100 px-4 pb-4"
                key={`accordion-content-${componentAction.workflowNodeName}`}
            >
                {outputSchemaExists ? (
                    <>
                        <DataPill
                            componentIcon={componentAction.componentDefinition.icon}
                            property={componentAction?.outputSchema}
                            root={true}
                            sampleOutput={sampleOutput}
                            workflowNodeName={componentAction.workflowNodeName}
                        />

                        <ul className="flex w-full flex-col space-y-2 border-l pl-4 group-data-[state=open]:h-full">
                            {filteredProperties?.map((property) => {
                                const value = getNestedObject(sampleOutput, property.name!.replaceAll('/', '.'));

                                return (
                                    <div className="flex items-center space-x-3" key={property.name}>
                                        <DataPill
                                            componentIcon={componentAction.componentDefinition.icon}
                                            property={property}
                                            sampleOutput={sampleOutput}
                                            workflowNodeName={componentAction.workflowNodeName}
                                        />

                                        {(value || value === 0 || value === false) && typeof value !== 'object' && (
                                            <div className="flex-1 text-xs text-muted-foreground">
                                                {value === true ? 'true' : value === false ? false : value}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </ul>
                    </>
                ) : (
                    <div className="flex flex-col gap-3">
                        <span className="font-semibold">Test component</span>

                        <p className="text-sm">
                            <span className="font-semibold">{currentComponentAction?.workflowNodeName} </span>
                            needs to be tested to generate an output schema. Please go to the &quot;Output&quot; tab of
                            the
                            <span className="font-semibold"> {currentComponentAction?.workflowNodeName} </span>{' '}
                            component.
                        </p>

                        <Button onClick={handleOutputTabRedirectClick} variant="secondary">
                            Go to
                            <span className="pl-1 font-semibold">{currentComponentAction?.workflowNodeName}</span>
                        </Button>
                    </div>
                )}
            </AccordionContent>
        </>
    );
};

export default DataPillPanelBodyPropertiesItem;
