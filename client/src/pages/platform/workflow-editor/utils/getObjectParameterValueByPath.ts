// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function getObjectParameterValueByPath(path: string, parameters: any) {
    if (!path || (parameters && !Object.keys(parameters).length)) {
        return;
    }

    if (path.includes('[')) {
        const pathSegments = path.split('.');

        let parameterValue = parameters;

        pathSegments.forEach((segment) => {
            if (segment.includes('[') && segment.includes(']')) {
                const arrayName = segment.substring(0, segment.indexOf('['));

                const index = parseInt(segment.substring(segment.indexOf('[') + 1, segment.indexOf(']')));

                parameterValue = parameterValue[arrayName][index];
            } else {
                parameterValue = parameterValue[segment];
            }
        });

        return parameterValue;
    }

    return path.split('.').reduce((parameters, key) => parameters?.[key], parameters);
}
