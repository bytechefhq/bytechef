import {PATH_DIGIT_PREFIX} from '@/shared/constants';
import isObject from 'isobject';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function formatKeysWithDigits(obj: any) {
    const formattedObj = {...obj};

    Object.keys(formattedObj).forEach((key) => {
        if (key.match(/^\d/)) {
            formattedObj[`${PATH_DIGIT_PREFIX}${key}`] = formattedObj[key];

            delete formattedObj[key];
        }

        if (isObject(formattedObj[key]) && formattedObj[key] !== null) {
            formattedObj[key] = formatKeysWithDigits(formattedObj[key]);
        }
    });

    return formattedObj;
}
