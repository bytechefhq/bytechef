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
    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);

    const {
        availablePropertyTypes,
        calculatedPath,
        getPropertyKey,
        handleAddItemClick,
        handleDeleteClick,
        isContainerObject,
        label,
        name,
        newPropertyName,
        newPropertyType,
        placeholder,
        setNewPropertyName,
        setNewPropertyType,
        subProperties,
    } = useObjectProperty({
        onDeleteClick,
        path,
        property,
    });

    return (
        <Fragment key={name}>
            <ul
                aria-label={`${name} object properties`}
                className={twMerge(
                    'space-y-4',
                    label && !isContainerObject && 'ml-2 border-l border-l-border/50',
                    arrayName && !isContainerObject && 'pl-2'
                )}
                role="list"
            >
                {(subProperties as unknown as Array<SubPropertyType>)?.map((subProperty, index) => (
                    <Property
                        arrayIndex={arrayIndex}
                        arrayName={arrayName}
                        customClassName={twMerge(
                            'w-full last-of-type:pb-0',
                            label && 'mb-0',
                            isContainerObject && 'pb-0',
                            !arrayName && !isContainerObject && 'pl-2'
                        )}
                        deletePropertyButton={
                            subProperty.custom && name && subProperty.name && currentComponent ? (
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

            {!!availablePropertyTypes?.length && (
                <SubPropertyPopover
                    availablePropertyTypes={availablePropertyTypes}
                    buttonLabel={placeholder}
                    handleClick={handleAddItemClick}
                    newPropertyName={newPropertyName}
                    newPropertyType={newPropertyType}
                    setNewPropertyName={setNewPropertyName}
                    setNewPropertyType={setNewPropertyType}
                />
            )}
        </Fragment>
    );
};

export default ObjectProperty;
