const ARRAY_ACCESS_PATTERN = /^(.*?)\[(index|\d+)\]$/;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getNestedObject = (jsonObject: any, selector: string) => {
    const selectors = selector.split('.');

    return selectors.reduce((object, key) => {
        if (!object) {
            return undefined;
        }

        try {
            let finalKey: string | undefined = key;
            let index = -1;

            const arrayAccessMatch = key.match(ARRAY_ACCESS_PATTERN);

            if (arrayAccessMatch) {
                const [, propertyName, arrayIndex] = arrayAccessMatch;

                index = arrayIndex === 'index' ? 0 : Number(arrayIndex);

                finalKey = propertyName || undefined;
            }

            if (Array.isArray(object) && index === -1) {
                index = 0;
            }

            if (index > -1) {
                if (finalKey) {
                    return Array.isArray(object) ? object[index][finalKey] : object[finalKey][index];
                } else {
                    return object[index];
                }
            } else {
                return finalKey ? object[finalKey] : undefined;
            }
        } catch {
            return undefined;
        }
    }, jsonObject);
};

export default getNestedObject;
