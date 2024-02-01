import {Button} from '@/components/ui/button';
import {DataPillType} from '@/types/types';
import {PlusIcon} from '@radix-ui/react-icons';
import DropdownMenu from 'components/DropdownMenu/DropdownMenu';
import {useState} from 'react';
import {PropertyType} from 'types/projectTypes';

import Property from './Property';

const ArrayProperty = ({dataPills, property}: {dataPills?: Array<DataPillType>; property: PropertyType}) => {
    const [arrayItems, setArrayItems] = useState(property.items);

    const {items, multipleValues} = property;

    const newPropertyTypeOptions = items?.reduce((uniqueItems: Array<{label: string; value: string}>, item) => {
        if (!uniqueItems.find((uniqueItem) => uniqueItem.value === item.type)) {
            if (item.type) {
                uniqueItems.push({
                    label: item.type,
                    value: item.type,
                });
            }
        }

        return uniqueItems;
    }, []);

    const handleAddItemClick = () => arrayItems && setArrayItems([...arrayItems, arrayItems[0]]);

    return (
        <ul className="w-full">
            {items?.map((item, index) => (
                <Property
                    customClassName="border-l ml-2 pl-2 pb-2 last-of-type:pb-0"
                    dataPills={dataPills}
                    key={`${item.name}_${index}`}
                    mention={!!dataPills?.length}
                    property={item}
                />
            ))}

            {multipleValues && (
                <div className="relative ml-2 w-full self-start border-l pl-2 pt-2">
                    {newPropertyTypeOptions?.length && newPropertyTypeOptions?.length > 1 ? (
                        <DropdownMenu
                            customTriggerComponent={
                                <Button
                                    className="rounded-sm bg-gray-100 text-sm font-medium hover:bg-gray-200"
                                    onClick={handleAddItemClick}
                                    size="sm"
                                    variant="ghost"
                                >
                                    <PlusIcon className="size-4" /> Add item
                                </Button>
                            }
                            menuItems={newPropertyTypeOptions}
                        />
                    ) : (
                        <Button
                            className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                            onClick={handleAddItemClick}
                            size="sm"
                            variant="ghost"
                        >
                            <PlusIcon className="size-4" /> Add item
                        </Button>
                    )}
                </div>
            )}
        </ul>
    );
};

export default ArrayProperty;
