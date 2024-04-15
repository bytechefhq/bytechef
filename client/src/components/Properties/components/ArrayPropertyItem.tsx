import {ControlTypeModel} from '@/middleware/platform/configuration';
import {
    ArrayPropertyType,
    ComponentType,
    CurrentComponentDefinitionType,
    DataPillType,
    PropertyType,
} from '@/types/types';

import Property from '../Property';
import DeletePropertyButton from './DeletePropertyButton';

interface ArrayPropertyItemProps {
    arrayItem: ArrayPropertyType;
    arrayName?: string;
    currentComponentDefinition?: CurrentComponentDefinitionType;
    currentComponent?: ComponentType;
    dataPills?: DataPillType[];
    index: number;
    onDeleteClick: (path: string, name: string, index: number) => void;
    path?: string;
    setArrayItems: React.Dispatch<React.SetStateAction<Array<ArrayPropertyType | Array<ArrayPropertyType>>>>;
}

const ArrayPropertyItem = ({
    arrayItem,
    arrayName,
    currentComponent,
    currentComponentDefinition,
    dataPills,
    index,
    onDeleteClick,
    path,
    setArrayItems,
}: ArrayPropertyItemProps) => {
    const handleOnDeleteClick = () => {
        onDeleteClick(`${path}.${arrayName}`, arrayItem.name!, index);

        setArrayItems((subProperties) =>
            subProperties.filter((_subProperty, subPropertyIndex) => subPropertyIndex !== index)
        );
    };

    return (
        <div className="ml-2 flex w-full items-center border-l pb-2 last-of-type:pb-0" key={arrayItem.name}>
            <Property
                arrayIndex={index}
                arrayName={arrayName}
                currentComponent={currentComponent}
                currentComponentDefinition={currentComponentDefinition}
                customClassName="pl-2 w-full"
                dataPills={dataPills}
                path={`${path}.${arrayName}`}
                property={arrayItem as PropertyType & {controlType?: ControlTypeModel; defaultValue?: string}}
            />

            {arrayItem.custom && arrayName && arrayItem.name && currentComponent && (
                <DeletePropertyButton
                    className="mx-2"
                    currentComponent={currentComponent}
                    onClick={handleOnDeleteClick}
                    propertyName={arrayName}
                    subPropertyIndex={index}
                />
            )}
        </div>
    );
};

export default ArrayPropertyItem;
