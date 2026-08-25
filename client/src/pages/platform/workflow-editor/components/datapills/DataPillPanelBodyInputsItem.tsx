import DataPill from '@/pages/platform/workflow-editor/components/datapills/DataPill';
import {PropertyType} from '@/shared/middleware/platform/configuration';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AccordionContent, AccordionTrigger} from '@radix-ui/react-accordion';
import {ChevronDownIcon, FormInputIcon} from 'lucide-react';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import getFieldMappingPillProperties from '../../utils/getFieldMappingPillProperties';

interface DataPillPanelBodyInputsItemProps {
    dataPillFilterQuery: string;
}

const DataPillPanelBodyInputsItem = ({dataPillFilterQuery}: DataPillPanelBodyInputsItemProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {data: workflowTestConfiguration} = useGetWorkflowTestConfigurationQuery(
        {
            environmentId: currentEnvironmentId,
            workflowId: workflow.id!,
        },
        !!workflow.id
    );

    if (!workflow.inputs || workflow.inputs.length === 0) {
        return <p className="text-sm">No defined inputs.</p>;
    }

    const filteredInputs = workflow.inputs.filter((input) =>
        input.name.toLowerCase().includes(dataPillFilterQuery.toLowerCase())
    );

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
                    {filteredInputs.map((input, index) => {
                        const sampleOutput = workflowTestConfiguration?.inputs?.[input.name];

                        const rootProperty = {
                            name: input.name,
                            type: input.type?.toUpperCase() as PropertyType,
                        };

                        if (input.type === 'field_mapping') {
                            const childProperties = getFieldMappingPillProperties(sampleOutput as string | undefined);

                            return (
                                <li className="flex w-full flex-col space-y-2" key={`${input.name}-${index}`}>
                                    <DataPill
                                        property={rootProperty}
                                        root
                                        sampleOutput={sampleOutput}
                                        workflowNodeName={input.name}
                                    />

                                    {childProperties.length > 0 && (
                                        <ul className="flex w-full flex-col space-y-2 border-l border-l-border/50 pl-4">
                                            {childProperties.map((childProperty, childIndex) => (
                                                <div
                                                    className="flex items-center space-x-3"
                                                    key={`${input.name}-${childProperty.name}-${childIndex}`}
                                                >
                                                    <DataPill
                                                        parentProperty={rootProperty}
                                                        path={childProperty.name}
                                                        property={childProperty}
                                                        sampleOutput={sampleOutput}
                                                        workflowNodeName={input.name}
                                                    />
                                                </div>
                                            ))}
                                        </ul>
                                    )}
                                </li>
                            );
                        }

                        return (
                            <li className="flex w-full items-center space-x-3" key={`${input.name}-${index}`}>
                                <DataPill
                                    property={rootProperty}
                                    root
                                    sampleOutput={sampleOutput}
                                    workflowNodeName={input.name}
                                />
                            </li>
                        );
                    })}
                </ul>
            </AccordionContent>
        </>
    );
};

export default DataPillPanelBodyInputsItem;
