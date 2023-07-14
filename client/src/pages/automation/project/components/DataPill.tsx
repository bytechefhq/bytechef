import {MouseEvent} from 'react';
import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

const DataPill = ({
    onClick,
    property,
}: {
    onClick: (event: MouseEvent<HTMLDivElement>) => void;
    property: PropertyType;
}) => {
    const hasSubProperties = !!property.properties?.length;

    return (
        <li
            draggable
            onDragStart={(event) =>
                event.dataTransfer.setData('name', property.name!)
            }
            className={twMerge(
                'mr-auto',
                hasSubProperties &&
                    'flex-col space-y-2 border-0 bg-transparent p-0 hover:cursor-default hover:bg-transparent'
            )}
        >
            {hasSubProperties ? (
                <fieldset className="flex flex-col rounded-lg border-2 border-gray-400 px-2 pb-2">
                    <legend className="px-2 pb-2 text-sm font-semibold uppercase text-gray-500">
                        {property.label}
                    </legend>

                    <ul className="flex flex-col space-y-2">
                        {property.properties?.map((subProperty) => (
                            <DataPill
                                key={subProperty.name}
                                onClick={onClick}
                                property={subProperty}
                            />
                        ))}
                    </ul>
                </fieldset>
            ) : (
                <div
                    className={twMerge(
                        'mr-auto flex cursor-pointer items-center rounded-full border border-gray-300 bg-white px-2 py-1 text-sm hover:bg-gray-50'
                    )}
                    data-name={property.name}
                    onClick={onClick}
                >
                    <span className="mr-2" title={property.type}>
                        {TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}
                    </span>

                    {property.label}
                </div>
            )}
        </li>
    );
};

export default DataPill;
