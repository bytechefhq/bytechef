import Button from '@/components/Button/Button';
import ArrayPropertyItem from '@/pages/platform/workflow-editor/components/properties/components/ArrayPropertyItem';
import SubPropertyPopover from '@/pages/platform/workflow-editor/components/properties/components/SubPropertyPopover';
import {useArrayProperty} from '@/pages/platform/workflow-editor/components/properties/hooks/useArrayProperty';
import {ArrayPropertyType, PropertyAllType} from '@/shared/types';
import {CircleAlertIcon, PlusIcon} from 'lucide-react';
import {Fragment, useCallback} from 'react';
import {twMerge} from 'tailwind-merge';

interface ArrayPropertyProps {
    onDeleteClick: (path: string) => void;
    parentArrayItems?: Array<ArrayPropertyType>;
    path: string;
    property: PropertyAllType;
}

const ArrayProperty = ({onDeleteClick, parentArrayItems, path, property}: ArrayPropertyProps) => {
    const {
        arrayConstraintHint,
        arrayItems,
        availablePropertyTypes,
        currentNode,
        defaultPropertyType,
        handleAddItemClick,
        handleDeleteClick,
        isAddDisabled,
        items,
        name,
        setArrayItems,
    } = useArrayProperty({onDeleteClick, parentArrayItems, path, property});

    const subPropertyPopoverVisible = availablePropertyTypes.length > 1 && !!defaultPropertyType;

    const lastArrayItemIndex = (arrayItems?.length ?? 0) - 1;

    const handleAddSingleTypeItemClick = useCallback(
        () => handleAddItemClick({name: '', type: defaultPropertyType ?? 'STRING'}),
        [defaultPropertyType, handleAddItemClick]
    );

    return (
        <Fragment key={`${path}_${name}_arrayProperty`}>
            <ul className="ml-2 flex flex-col gap-2 border-l border-l-border/50">
                {arrayItems?.map((arrayItem, index) =>
                    Array.isArray(arrayItem) ? (
                        arrayItem.map((subItem: ArrayPropertyType, subItemIndex: number) => (
                            <ArrayPropertyItem
                                arrayItem={subItem}
                                arrayName={name}
                                className={twMerge(
                                    index === lastArrayItemIndex && subItemIndex === arrayItem.length - 1 && 'mb-2'
                                )}
                                currentNode={currentNode}
                                index={index}
                                key={`${subItem.key}_${path}[${index}]_${subItem.name}`}
                                onDeleteClick={handleDeleteClick}
                                parentArrayItems={items}
                                path={path}
                                setArrayItems={setArrayItems}
                            />
                        ))
                    ) : (
                        <ArrayPropertyItem
                            arrayItem={arrayItem}
                            arrayName={name}
                            className={twMerge(index === lastArrayItemIndex && 'mb-2')}
                            currentNode={currentNode}
                            index={index}
                            key={`${arrayItem.key}_${path}[${index}]`}
                            onDeleteClick={handleDeleteClick}
                            parentArrayItems={items}
                            path={`${path}[${index}]`}
                            setArrayItems={setArrayItems}
                        />
                    )
                )}
            </ul>

            <div className="flex items-center gap-3">
                {subPropertyPopoverVisible ? (
                    <SubPropertyPopover
                        array
                        availablePropertyTypes={availablePropertyTypes}
                        buttonLabel={property.placeholder ?? parentArrayItems?.[0]?.placeholder}
                        defaultPropertyType={defaultPropertyType}
                        disabled={isAddDisabled}
                        handleClick={handleAddItemClick}
                        insideConditionTaskDispatcher={currentNode?.componentName === 'condition'}
                        key={`${path}_${name}_subPropertyPopoverButton`}
                    />
                ) : (
                    <Button
                        className="mb-2 rounded-sm"
                        disabled={isAddDisabled}
                        icon={<PlusIcon />}
                        key={`${path}_${name}_addPropertyPopoverButton`}
                        label={property.placeholder || 'Add array item'}
                        onClick={handleAddSingleTypeItemClick}
                        size="sm"
                        variant="secondary"
                    />
                )}

                {arrayConstraintHint.variant !== 'none' && (
                    <div className={twMerge('flex items-center gap-1.5', arrayConstraintHint.textColor)}>
                        <CircleAlertIcon aria-hidden="true" className="size-4" />

                        <span className="text-sm">{arrayConstraintHint.text}</span>
                    </div>
                )}
            </div>
        </Fragment>
    );
};

export default ArrayProperty;
