import {TYPE_ICONS} from 'shared/typeIcons';
import {twMerge} from 'tailwind-merge';
import {PropertyType} from 'types/projectTypes';

const DataPill = ({
    onClick,
    property,
}: {
    onClick: () => void;
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
                'mr-auto flex cursor-pointer items-center rounded-xl border border-gray-300 bg-white px-2 py-1 text-sm hover:bg-gray-50',
                hasSubProperties &&
                    'flex-col space-y-2 border-0 bg-transparent p-0 hover:cursor-default hover:bg-transparent'
            )}
        >
            {hasSubProperties ? (
                <ul>
                    {property.properties?.map((subProperty) => (
                        <DataPill
                            key={subProperty.name}
                            onClick={onClick}
                            property={subProperty}
                        />
                    ))}
                </ul>
            ) : (
                <div className="flex items-center" onClick={onClick}>
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
