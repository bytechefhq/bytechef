export const objectToQuery = (object: Record<string, string>) => {
    return new URLSearchParams(object).toString();
};

export const queryToObject = (query: string) => {
    const parameters = new URLSearchParams(query);

    return Object.fromEntries(parameters.entries());
};
