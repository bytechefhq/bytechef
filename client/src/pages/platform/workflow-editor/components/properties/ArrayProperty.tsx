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
        currentComponent,
        handleAddItemClick,
        handleDeleteClick,
        isAddDisabled,
        items,
        name,
        newPropertyType,
        setArrayItems,
        setNewPropertyType,
    } = useArrayProperty({onDeleteClick, parentArrayItems, path, property});

    return (
        <Fragment key={`${path}_${name}_arrayProperty`}>
            <ul className="ml-2 flex flex-col space-y-4 border-l border-l-border/50">
                {arrayItems?.map((arrayItem, index) =>
                    Array.isArray(arrayItem) ? (
                        arrayItem.map((subItem: ArrayPropertyType) => (
                            <ArrayPropertyItem
                                arrayItem={subItem}
                                arrayName={name}
                                currentComponent={currentComponent}
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
                            currentComponent={currentComponent}
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

            <div className="mt-3 flex items-center gap-3">
                {availablePropertyTypes.length > 1 && !!newPropertyType ? (
                    <SubPropertyPopover
                        array
                        availablePropertyTypes={availablePropertyTypes}
                        buttonLabel={property.placeholder ?? parentArrayItems?.[0]?.placeholder}
                        disabled={isAddDisabled}
                        handleClick={handleAddItemClick}
                        insideConditionTaskDispatcher={currentComponent?.componentName === 'condition'}
                        key={`${path}_${name}_subPropertyPopoverButton`}
                        newPropertyType={newPropertyType}
                        setNewPropertyType={setNewPropertyType}
                    />
                ) : (
                    <Button
                        className="rounded-sm"
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
