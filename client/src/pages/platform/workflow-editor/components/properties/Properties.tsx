import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {GetClusterElementParameterDisplayConditions200Response} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {UseQueryResult} from '@tanstack/react-query';
import {ChevronDownIcon} from 'lucide-react';
import {Control, FieldValues, FormState} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';

import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';

interface PropertiesProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    controlPath?: string;
    displayConditionsQuery?: UseQueryResult<GetClusterElementParameterDisplayConditions200Response, Error>;
    customClassName?: string;
    operationName?: string;
    formState?: FormState<FieldValues>;
    path?: string;
    properties: Array<PropertyAllType>;
}

const Properties = ({
    control,
    controlPath,
    customClassName,
    displayConditionsQuery,
    formState,
    operationName,
    path,
    properties,
}: PropertiesProps) => {
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);

    const advancedProperties = properties.filter((property) => {
        const {advancedOption, hidden, name} = property;

        if (!name || !advancedOption) {
            return false;
        }

        if (!control && hidden) {
            return false;
        }

        return true;
    });

    const simpleProperties = properties.filter((property) => {
        const {advancedOption, hidden, name} = property;

        if (!name || advancedOption) {
            return false;
        }

        if (!control && hidden) {
            return false;
        }

        return true;
    });

    return (
        <>
            <ul
                className={twMerge('space-y-4', customClassName)}
                key={`${currentNode?.workflowNodeName}_${currentNode?.operationName}_properties`}
            >
                {simpleProperties.map((property, index) => (
                    <Property
                        control={control}
                        controlPath={controlPath}
                        displayConditionsQuery={displayConditionsQuery}
                        formState={formState}
                        key={`${currentNode?.workflowNodeName}_${currentNode?.operationName}_${property.name}_${index}`}
                        operationName={operationName}
                        path={path}
                        property={property}
                    />
                ))}
            </ul>

            {!!advancedProperties.length && (
                <Collapsible className="group flex w-full flex-col justify-center">
                    <CollapsibleTrigger asChild>
                        <Button className="mx-4" variant="outline">
                            <span>Show Advanced Properties</span>

                            <ChevronDownIcon className="size-4 transition-all group-data-[state=open]:rotate-180" />
                        </Button>
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                        <ul className="space-y-4 p-4" key={`${currentNode?.operationName}_advancedProperties`}>
                            {advancedProperties.map((property, index) => (
                                <Property
                                    control={control}
                                    controlPath={controlPath}
                                    displayConditionsQuery={displayConditionsQuery}
                                    formState={formState}
                                    key={`${property.name}_${currentNode?.operationName}_${index}`}
                                    operationName={operationName}
                                    path={path}
                                    property={property}
                                />
                            ))}
                        </ul>
                    </CollapsibleContent>
                </Collapsible>
            )}
        </>
    );
};

export default Properties;
