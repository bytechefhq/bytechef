export default function getPropertyKey(name: string | undefined, displayCondition?: string): string {
    if (!name) {
        return '';
    }

    return displayCondition ? `${name}::${displayCondition}` : name;
}
