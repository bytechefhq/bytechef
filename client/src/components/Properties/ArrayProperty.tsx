import {PlusIcon} from '@heroicons/react/24/outline';
import Button from 'components/Button/Button';
import DropdownMenu from 'components/DropdownMenu/DropdownMenu';
import {PropertyType} from 'types/projectTypes';

import {Property} from './Properties';

const ArrayProperty = ({property}: {property: PropertyType}) => {
    const {items} = property;

    const formattedArrayItems = items?.map((item) => ({
        label: item.type,
        value: item.type,
    }));

    return (
        <>
            <ul className="w-full">
                {items?.map((item, index) => (
                    <Property
                        customClassName="border-l ml-2 pl-2 last-of-type:mb-0"
                        key={`${item.name}_${index}`}
                        property={item}
                    />
                ))}

                <div className="relative ml-2 w-full self-start border-l pl-2 pt-2">
                    {formattedArrayItems?.length &&
                    formattedArrayItems?.length > 1 ? (
                        <DropdownMenu
                            customTriggerComponent={
                                <Button
                                    className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                                    displayType="unstyled"
                                    icon={<PlusIcon className="h-4 w-4" />}
                                    iconPosition="left"
                                    label="Add item"
                                    onClick={() =>
                                        console.log(
                                            'update the workflow definition with a new property'
                                        )
                                    }
                                    size="small"
                                />
                            }
                            menuItems={formattedArrayItems}
                        />
                    ) : (
                        <Button
                            className="rounded-sm bg-gray-100 text-xs font-medium hover:bg-gray-200"
                            displayType="unstyled"
                            icon={<PlusIcon className="h-4 w-4" />}
                            iconPosition="left"
                            label="Add item"
                            onClick={() =>
                                console.log(
                                    'update the workflow definition with a new property'
                                )
                            }
                            size="small"
                        />
                    )}
                </div>
            </ul>
        </>
    );
};

export default ArrayProperty;
