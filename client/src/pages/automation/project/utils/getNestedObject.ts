// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getNestedObject = (jsonObject: any, selector: string) => {
    const selectors = selector.split('.');

    return selectors.reduce((object, key) => {
        if (!object) {
            return undefined;
        }

        let finalKey: string | undefined = key;
        let index = -1;

        if (finalKey === '[index]') {
            index = 0;

            finalKey = undefined;
        } else if (finalKey.endsWith('[index]')) {
            index = 0;

            finalKey = finalKey.substring(0, finalKey.length - '[index]'.length);
        }

        if (Array.isArray(object)) {
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
    }, jsonObject);
};

export default getNestedObject;
