export type PropertySaveOverrideType = (value: unknown) => void;

const propertySaveOverrides = new Map<string, PropertySaveOverrideType>();

function getOverrideKey(workflowNodeName: string, path: string): string {
    return `${workflowNodeName}\u0000${path}`;
}

interface RegisterPropertySaveOverrideProps {
    path: string;
    save: PropertySaveOverrideType;
    workflowNodeName: string;
}

export function registerPropertySaveOverride({
    path,
    save,
    workflowNodeName,
}: RegisterPropertySaveOverrideProps): () => void {
    const overrideKey = getOverrideKey(workflowNodeName, path);

    propertySaveOverrides.set(overrideKey, save);

    return () => {
        if (propertySaveOverrides.get(overrideKey) === save) {
            propertySaveOverrides.delete(overrideKey);
        }
    };
}

export function getPropertySaveOverride(
    workflowNodeName: string | undefined,
    path: string
): PropertySaveOverrideType | undefined {
    if (!workflowNodeName) {
        return undefined;
    }

    return propertySaveOverrides.get(getOverrideKey(workflowNodeName, path));
}
