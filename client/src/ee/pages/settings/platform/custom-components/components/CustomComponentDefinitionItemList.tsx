interface CustomComponentDefinitionItemI {
    description?: string | null;
    name: string;
    title?: string | null;
}

interface CustomComponentDefinitionItemListProps {
    emptyMessage: string;
    items: Array<CustomComponentDefinitionItemI>;
}

const CustomComponentDefinitionItemList = ({emptyMessage, items}: CustomComponentDefinitionItemListProps) =>
    items.length > 0 ? (
        <ul className="divide-y divide-stroke-neutral-primary">
            {items.map((item) => (
                <li className="py-2" key={item.name}>
                    <div className="text-sm font-medium">{item.title || item.name}</div>

                    <div className="text-xs text-content-neutral-secondary">{item.name}</div>

                    {item.description && (
                        <div className="mt-1 text-xs text-content-neutral-secondary">{item.description}</div>
                    )}
                </li>
            ))}
        </ul>
    ) : (
        <p className="text-sm text-content-neutral-secondary">{emptyMessage}</p>
    );

export default CustomComponentDefinitionItemList;
