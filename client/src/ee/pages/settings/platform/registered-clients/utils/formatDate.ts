const formatDate = (value: unknown): string => {
    if (value == null) {
        return '';
    }

    const date = new Date(value as number);

    return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
};

export default formatDate;
