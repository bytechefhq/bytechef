import Button from '@/components/Button/Button';
import ArrayPropertyItem from '@/pages/platform/workflow-editor/components/properties/components/ArrayPropertyItem';
import SubPropertyPopover from '@/pages/platform/workflow-editor/components/properties/components/SubPropertyPopover';
import {useArrayProperty} from '@/pages/platform/workflow-editor/components/properties/hooks/useArrayProperty';
import {ArrayPropertyType, PropertyAllType} from '@/shared/types';
import {CircleAlertIcon, PlusIcon} from 'lucide-react';
import {Fragment} from 'react';
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
        handleAddItemClick,
        handleDeleteClick,
        isAddDisabled,
        items,
        name,
        newPropertyType,
        setArrayItems,
        setNewPropertyType,
    } = useArrayProperty({onDeleteClick, parentArrayItems, path, property});

    const subPropertyPopoverVisible = availablePropertyTypes.length > 1 && !!newPropertyType;

    const lastArrayItemIndex = (arrayItems?.length ?? 0) - 1;

    return (
        <Fragment key={`${path}_${name}_arrayProperty`}>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {arrayItems?.map((arrayItem, index) =>
                    Array.isArray(arrayItem) ? (
                        arrayItem.map((subItem: ArrayPropertyType, subItemIndex: number) => (
                            <ArrayPropertyItem
                                arrayItem={subItem}
                                arrayName={name}
                                className={twMerge(
                                    subPropertyPopoverVisible &&
                                        index === lastArrayItemIndex &&
                                        subItemIndex === arrayItem.length - 1 &&
                                        'mb-2'
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
                            className={twMerge(subPropertyPopoverVisible && index === lastArrayItemIndex && 'mb-2')}
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
                        disabled={isAddDisabled}
                        handleClick={handleAddItemClick}
                        insideConditionTaskDispatcher={currentNode?.componentName === 'condition'}
                        key={`${path}_${name}_subPropertyPopoverButton`}
                        newPropertyType={newPropertyType}
                        setNewPropertyType={setNewPropertyType}
                    />
                ) : (
                    <Button
                        className="mb-2 rounded-sm"
                        disabled={isAddDisabled}
                        icon={<PlusIcon />}
                        key={`${path}_${name}_addPropertyPopoverButton`}
                        label={property.placeholder || 'Add array item'}
                        onClick={handleAddItemClick}
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
