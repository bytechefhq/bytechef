import DataPill from '@/pages/automation/project/components/DataPill';
import {PropertyType} from '@/types/projectTypes';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {ComponentActionData} from './DataPillPanelBody';

const DataPillPanelBodyItem = ({
    componentAction,
    filteredProperties,
}: {
    componentAction: ComponentActionData;
    filteredProperties: Array<PropertyType>;
}) => {
    const {icon, title} = componentAction.componentDefinition;

    const {componentActions} = useWorkflowDataStore();

    const currentComponentAction = componentActions.find(
        (action) => action.workflowAlias === componentAction.workflowAlias
    );

    return (
        <>
            <AccordionTrigger
                className="group flex w-full items-center justify-between border-gray-100 bg-white p-4 group-data-[state=closed]:border-b"
                key={`accordion-trigger-${componentAction.workflowAlias}`}
            >
                <div className="flex items-center space-x-4">
                    {icon && (
                        <div className="flex h-5 w-5 items-center">
                            <InlineSVG src={icon} />
                        </div>
                    )}

                    <span className="text-sm">
                        {title}

                        <span className="pl-1 text-xs text-gray-400">
                            ({componentAction.workflowAlias})
                        </span>
                    </span>
                </div>

                <span className="ml-auto mr-4 rounded bg-muted px-2 py-1 text-xs">
                    {currentComponentAction?.actionName}
                </span>

                <ChevronDownIcon className="h-5 w-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
            </AccordionTrigger>

            <AccordionContent
                className="h-full w-full space-y-2 border-b border-gray-100 px-4 pb-4"
                key={`accordion-content-${componentAction.workflowAlias}`}
            >
                <DataPill
                    componentAlias={componentAction.workflowAlias}
                    componentIcon={componentAction.componentDefinition.icon}
                />

                <ul className="flex w-full flex-col space-y-2 border-l pl-4 group-data-[state=open]:h-full">
                    {filteredProperties?.map((property) => (
                        <DataPill
                            componentAlias={componentAction.workflowAlias}
                            componentIcon={
                                componentAction.componentDefinition.icon
                            }
                            key={property.name}
                            property={property}
                        />
                    ))}
                </ul>
            </AccordionContent>
        </>
    );
};

export default DataPillPanelBodyItem;
