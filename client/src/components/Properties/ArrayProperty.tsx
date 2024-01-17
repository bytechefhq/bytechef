import {Button} from '@/components/ui/button';
import {DataPillType} from '@/types/types';
import {PlusIcon} from '@radix-ui/react-icons';
import DropdownMenu from 'components/DropdownMenu/DropdownMenu';
import {PropertyType} from 'types/projectTypes';

import Property from './Property';

const ArrayProperty = ({dataPills, property}: {dataPills?: Array<DataPillType>; property: PropertyType}) => {
    const {items} = property;

    const formattedArrayItems = items?.map((item) => ({
        label: item.type,
        value: item.type,
    }));

    return (
        <ul className="w-full">
            {items?.map((item, index) => (
                <Property
                    customClassName="border-l ml-2 pl-2 last-of-type:mb-0"
                    dataPills={dataPills}
                    key={`${item.name}_${index}`}
                    mention={!!dataPills?.length}
                    property={item}
                />
            ))}

            <div className="relative ml-2 w-full self-start border-l pl-2 pt-2">
                {formattedArrayItems?.length && formattedArrayItems?.length > 1 ? (
                    <DropdownMenu
                        customTriggerComponent={
                            <Button
                                className="rounded-sm bg-gray-100 text-sm font-medium hover:bg-gray-200"
                                onClick={() => console.log('update the workflow definition with a new property')}
                                size="sm"
                                variant="ghost"
                            >
                                <PlusIcon className="size-4" /> Add item
                            </Button>
                        }
                        menuItems={formattedArrayItems}
                    />
                ) : (
                    <Button
                        className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                        onClick={() => console.log('update the workflow definition with a new property')}
                        size="sm"
                        variant="ghost"
                    >
                        <PlusIcon className="size-4" /> Add item
                    </Button>
                )}
            </div>
        </ul>
    );
};

export default ArrayProperty;
