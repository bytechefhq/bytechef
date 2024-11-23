import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {PropertyAllType} from '@/shared/types';
import {ChevronDownIcon} from 'lucide-react';
import {Fragment} from 'react';
import {FieldValues} from 'react-hook-form/dist/types';
import {Control, FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';

import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';
import Property from './Property';

interface PropertiesProps {
    /* eslint-disable @typescript-eslint/no-explicit-any */
    control?: Control<any, any>;
    controlPath?: string;
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
    formState,
    operationName,
    path,
    properties,
}: PropertiesProps) => {
    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const advancedProperties = properties.filter((property) => {
        const {advancedOption, displayCondition, hidden, name} = property;

        if (!name || !advancedOption || hidden) {
            return false;
        }

        if (displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
            return false;
        }

        return true;
    });

    const simpleProperties = properties.filter(
        (property) => property.name && !property.advancedOption && !property.hidden
    );

    return (
        <>
            <ul
                className={twMerge('space-y-4', customClassName)}
                key={`${currentComponent?.workflowNodeName}_${currentComponent?.operationName}_properties`}
            >
                {simpleProperties.map((property, index) => {
                    const {displayCondition, name} = property;

                    if (displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
                        return <Fragment key={`${name}_${index}`}></Fragment>;
                    }

                    return (
                        <Property
                            control={control}
                            controlPath={controlPath}
                            formState={formState}
                            key={`${currentComponent?.workflowNodeName}_${currentComponent?.operationName}_${name}_${index}`}
                            operationName={operationName}
                            path={path}
                            property={property}
                        />
                    );
                })}
            </ul>

            {!!advancedProperties.length && (
                <Collapsible className="group flex w-full flex-col justify-center">
                    <CollapsibleTrigger className="mx-4 flex items-center justify-center rounded-md px-4 py-2 text-center hover:bg-gray-100">
                        <h2 className="text-sm text-gray-500">Advanced Properties</h2>

                        <ChevronDownIcon className="ml-2 size-4 text-gray-500 transition-all group-data-[state=open]:rotate-180" />
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                        <ul className="space-y-4 p-4" key={`${currentComponent?.operationName}_advancedProperties`}>
                            {advancedProperties.map((property, index) => {
                                const {displayCondition, name} = property;

                                if (displayCondition && !currentComponent?.displayConditions?.[displayCondition]) {
                                    return <Fragment key={`${name}_${index}`}></Fragment>;
                                }

                                return (
                                    <Property
                                        control={control}
                                        controlPath={controlPath}
                                        formState={formState}
                                        key={`${name}_${currentComponent?.operationName}_${index}`}
                                        operationName={operationName}
                                        path={path}
                                        property={property}
                                    />
                                );
                            })}
                        </ul>
                    </CollapsibleContent>
                </Collapsible>
            )}
        </>
    );
};

export default Properties;
