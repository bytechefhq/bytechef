// eslint-disable-next-line @typescript-eslint/no-explicit-any
const getNestedObject = (jsonObj: any, selector: string) => {
    const selectors = selector.split('.');

    return selectors.reduce((obj, key) => {
        let finalKey: string | undefined = key;
        let index = -1;

        if (finalKey === '[index]') {
            index = 0;
            finalKey = undefined;
        } else if (finalKey.endsWith('[index]')) {
            index = 0;
            finalKey = finalKey.substring(0, finalKey.length - '[index]'.length);
        }

        if (Array.isArray(obj)) {
            index = 0;
        }

        return obj
            ? index > -1
                ? finalKey
                    ? Array.isArray(obj)
                        ? obj[index][finalKey]
                        : obj[finalKey][index]
                    : obj[index]
                : finalKey
                  ? obj[finalKey]
                  : undefined
            : undefined;
    }, jsonObj);
};

export default getNestedObject;
