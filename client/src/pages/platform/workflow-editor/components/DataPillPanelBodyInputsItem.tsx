import DataPill from '@/pages/platform/workflow-editor/components/DataPill';
import {PropertyTypeModel} from '@/shared/middleware/platform/configuration';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon, FormInputIcon} from 'lucide-react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

const DataPillPanelBodyInputsItem = () => {
    const {workflow} = useWorkflowDataStore();

    return workflow.inputs && workflow.inputs.length > 0 ? (
        <>
            <AccordionTrigger className="group flex w-full items-center justify-between border-gray-100 bg-white p-4 group-data-[state=closed]:border-b">
                <div className="flex items-center space-x-4">
                    <div className="flex size-5 items-center">
                        <FormInputIcon />
                    </div>

                    <span className="text-sm">Inputs</span>
                </div>

                <ChevronDownIcon className="size-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
            </AccordionTrigger>
            <AccordionContent className="size-full space-y-2 border-b border-gray-100 px-4 pb-4">
                <ul className="flex w-full flex-col space-y-2 border-l pl-4 group-data-[state=open]:h-full">
                    {workflow.inputs?.map((input) => {
                        return (
                            <div className="flex items-center space-x-3" key={input.name}>
                                <DataPill
                                    componentIcon={''}
                                    property={{
                                        name: input.name,
                                        type: input.type?.toUpperCase() as PropertyTypeModel,
                                    }}
                                    root={true}
                                    sampleOutput={undefined}
                                    workflowNodeName={input.name}
                                />
                            </div>
                        );
                    })}
                </ul>

                <p className="text-sm">No defined inputs.</p>
            </AccordionContent>{' '}
        </>
    ) : (
        <></>
    );
};

export default DataPillPanelBodyInputsItem;
