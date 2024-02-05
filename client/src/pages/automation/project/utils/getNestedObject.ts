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

        if (obj) {
            if (index > -1) {
                if (finalKey) {
                    if (Array.isArray(obj)) {
                        return obj[index][finalKey];
                    } else {
                        return obj[finalKey][index];
                    }
                } else {
                    return obj[index];
                }
            } else {
                if (finalKey) {
                    return obj[finalKey];
                } else {
                    return undefined;
                }
            }
        } else {
            return undefined;
        }
    }, jsonObj);
};

export default getNestedObject;
