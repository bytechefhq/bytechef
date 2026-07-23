import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType, SubPropertyType} from '@/shared/types';
import {Fragment} from 'react';
import {twMerge} from 'tailwind-merge';

import Property from './Property';
import DeletePropertyButton from './components/DeletePropertyButton';
import SubPropertyPopover from './components/SubPropertyPopover';
import {useObjectProperty} from './hooks/useObjectProperty';

interface ObjectPropertyProps {
    operationName?: string;
    arrayIndex?: number;
    arrayName?: string;
    onDeleteClick?: (path: string) => void;
    path?: string;
    property: PropertyAllType;
}

const ObjectProperty = ({arrayIndex, arrayName, onDeleteClick, operationName, path, property}: ObjectPropertyProps) => {
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);

    const {
        availablePropertyTypes,
        calculatedPath,
        defaultPropertyType,
        existingSubPropertyNames,
        getPropertyKey,
        handleAddItemClick,
        handleDeleteClick,
        isContainerObject,
        label,
        name,
        placeholder,
        subProperties,
    } = useObjectProperty({
        onDeleteClick,
        path,
        property,
    });

    const subPropertyList = subProperties as unknown as Array<SubPropertyType> | undefined;

    const subPropertyPopoverVisible = !!availablePropertyTypes?.length;

    const lastSubPropertyIndex = (subPropertyList?.length ?? 0) - 1;

    return (
        <Fragment key={name}>
            <ul
                aria-label={`${name} object properties`}
                className={twMerge(
                    'gap-2 space-y-4',
                    label && !isContainerObject && 'ml-2 flex flex-col border-l border-l-border/50',
                    arrayName && !isContainerObject && 'pl-2'
                )}
                role="list"
            >
                {subPropertyList?.map((subProperty, index) => (
                    <Property
                        arrayIndex={arrayIndex}
                        arrayName={arrayName}
                        customClassName={twMerge(
                            'w-full last-of-type:pb-0',
                            label && 'mb-0',
                            isContainerObject && 'pb-0',
                            !arrayName && !isContainerObject && 'pl-2',
                            subPropertyPopoverVisible && index === lastSubPropertyIndex && 'mb-2'
                        )}
                        deletePropertyButton={
                            subProperty.custom && name && subProperty.name && currentNode ? (
                                <DeletePropertyButton
                                    onClick={() => handleDeleteClick(subProperty)}
                                    propertyName={subProperty.label ?? subProperty.name}
                                />
                            ) : undefined
                        }
                        key={
                            getPropertyKey(subProperty.name, subProperty.displayCondition) ||
                            `${property.name}_${subProperty.name}_${index}`
                        }
                        objectName={arrayName ? '' : name}
                        operationName={operationName}
                        path={`${calculatedPath}.${subProperty.name}`}
                        property={subProperty}
                    />
                ))}
            </ul>

            {subPropertyPopoverVisible && (
                <SubPropertyPopover
                    availablePropertyTypes={availablePropertyTypes}
                    buttonLabel={placeholder}
                    defaultPropertyType={defaultPropertyType}
                    existingPropertyNames={existingSubPropertyNames}
                    handleClick={handleAddItemClick}
                    propertyName={name}
                />
            )}
        </Fragment>
    );
};

export default ObjectProperty;
