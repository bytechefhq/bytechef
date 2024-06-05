import {ArrayPropertyType, ComponentType} from '@/shared/types';

import Property from '../Property';
import DeletePropertyButton from './DeletePropertyButton';

interface ArrayPropertyItemProps {
    arrayItem: ArrayPropertyType;
    arrayName?: string;
    currentComponent?: ComponentType;
    index: number;
    onDeleteClick: (path: string) => void;
    path: string;
    setArrayItems: React.Dispatch<React.SetStateAction<Array<ArrayPropertyType | Array<ArrayPropertyType>>>>;
}

const ArrayPropertyItem = ({
    arrayItem,
    arrayName,
    currentComponent,
    index,
    onDeleteClick,
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
        <div className="ml-2 flex w-full items-center" key={`${arrayName}_${arrayItem.name}`}>
            <Property
                arrayIndex={index}
                arrayName={arrayName}
                customClassName="pl-2 w-full"
                inputTypeSwitchButtonClassName="ml-auto"
                key={`${arrayName}_${arrayItem.name}_property`}
                parameterValue={arrayItem.defaultValue}
                path={path}
                property={arrayItem as ArrayPropertyType}
            />

            {arrayItem.custom && arrayName && arrayItem.name && currentComponent && (
                <DeletePropertyButton className="ml-2 mr-4" onClick={handleOnDeleteClick} propertyName={path} />
            )}
        </div>
    );
};

export default ArrayPropertyItem;
