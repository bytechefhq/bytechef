import DataPill from '@/pages/platform/workflow-editor/components/datapills/DataPill';
import {PropertyType} from '@/shared/middleware/platform/configuration';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon, FormInputIcon} from 'lucide-react';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';

const DataPillPanelBodyInputsItem = () => {
    const workflow = useWorkflowDataStore((state) => state.workflow);

    if (!workflow.inputs || workflow.inputs.length === 0) {
        return <p className="text-sm">No defined inputs.</p>;
    }

    return (
        <>
            <AccordionTrigger className="group flex w-full items-center justify-between border-border/50 bg-surface-main p-4 group-data-[state=closed]:border-b">
                <div className="flex items-center space-x-4">
                    <FormInputIcon className="size-5" />

                    <span className="text-sm">Inputs</span>
                </div>

                <ChevronDownIcon className="size-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
            </AccordionTrigger>

            <AccordionContent className="size-full space-y-2 border-b border-b-border/50 px-4 pb-4">
                <ul className="flex w-full flex-col space-y-2 border-l border-l-border/50 pl-4 group-data-[state=open]:h-full">
                    {workflow.inputs.map((input, index) => (
                        <li className="flex w-full items-center space-x-3" key={`${input.name}-${index}`}>
                            <DataPill
                                property={{
                                    name: input.name,
                                    type: input.type?.toUpperCase() as PropertyType,
                                }}
                                root
                                sampleOutput={undefined}
                                workflowNodeName={input.name}
                            />
                        </li>
                    ))}
                </ul>
            </AccordionContent>
        </>
    );
};

export default DataPillPanelBodyInputsItem;
