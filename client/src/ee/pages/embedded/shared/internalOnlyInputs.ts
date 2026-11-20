export const REQUIRED_INTERNAL_ONLY_INPUT_HINT = 'Required before this workflow can be enabled';

export const isEmptyInternalOnlyInputValue = (value: unknown): boolean => {
    if (value === undefined || value === null) {
        return true;
    }

    if (typeof value === 'string') {
        return !value.trim();
    }

    if (typeof value === 'object') {
        return Object.values(value).every((memberValue) => isEmptyInternalOnlyInputValue(memberValue));
    }

    return false;
};
