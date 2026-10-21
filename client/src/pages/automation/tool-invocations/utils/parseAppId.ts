const POSITIVE_INTEGER_PATTERN = /^[1-9]\d*$/;

export const parseAppId = (value: string | null | undefined): number | undefined =>
    value && POSITIVE_INTEGER_PATTERN.test(value) && Number.isSafeInteger(Number(value)) ? Number(value) : undefined;
