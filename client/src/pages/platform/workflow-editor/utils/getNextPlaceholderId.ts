export function getNextConditionPlaceholderId(currentId: string): string {
    const parts = currentId.split('-');

    const sideIndex = parts.findIndex((part) => part === 'left' || part === 'right');

    if (sideIndex !== -1) {
        const numberIndex = sideIndex + 2;

        parts[numberIndex] = (parseInt(parts[numberIndex], 10) + 1).toString();
    }

    return parts.join('-');
}

export function getNextLoopPlaceholderId(currentId: string): string {
    const parts = currentId.split('-');

    const placeholderIndex = parts.findIndex((part) => part === 'placeholder');

    if (placeholderIndex !== -1) {
        const numberIndex = placeholderIndex + 1;

        parts[numberIndex] = (parseInt(parts[numberIndex], 10) + 1).toString();
    }

    return parts.join('-');
}
