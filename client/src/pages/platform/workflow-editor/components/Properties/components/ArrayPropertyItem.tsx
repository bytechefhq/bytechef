import {ArrayPropertyType, ComponentType} from '@/shared/types';

import Property from '../Property';
import DeletePropertyButton from './DeletePropertyButton';

interface ArrayPropertyItemProps {
    arrayItem: ArrayPropertyType;
    arrayName?: string;
    currentComponent?: ComponentType;
    index: number;
    onDeleteClick: (path: string) => void;
    parentArrayItems?: Array<ArrayPropertyType>;
    path: string;
    setArrayItems: React.Dispatch<React.SetStateAction<Array<ArrayPropertyType | Array<ArrayPropertyType>>>>;
}

const ArrayPropertyItem = ({
    arrayItem,
    arrayName,
    currentComponent,
    index,
    onDeleteClick,
    parentArrayItems,
    path,
    setArrayItems,
}: ArrayPropertyItemProps) => {
    const handleOnDeleteClick = () => {
        onDeleteClick(path);

        setArrayItems((subProperties) =>
            subProperties.filter((_subProperty, subPropertyIndex) => subPropertyIndex !== index)
        );
    };

    return (
        <div className="flex" key={`${arrayName}_${arrayItem.name}`}>
            <Property
                arrayIndex={index}
                arrayName={arrayName}
                customClassName="pl-2 w-full"
                deletePropertyButton={
                    arrayItem.custom && arrayName && arrayItem.name && currentComponent ? (
                        <DeletePropertyButton
                            key={`${arrayItem.key}_deleteSubPropertyButton`}
                            onClick={handleOnDeleteClick}
                            propertyName={path}
                        />
                    ) : undefined
                }
                key={`${arrayName}_${arrayItem.name}_${arrayItem.key}_property`}
                parameterValue={arrayItem.defaultValue}
                parentArrayItems={parentArrayItems}
                path={path}
                property={arrayItem as ArrayPropertyType}
            />
        </div>
    );
};

export default ArrayPropertyItem;
