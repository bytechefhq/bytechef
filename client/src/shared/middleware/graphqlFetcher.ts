import {endpointUrl, fetchParams} from './config';

export function fetcher<TData, TVariables>(
    query: string | {toString(): string},
    variables?: TVariables,
) {
    return async (): Promise<TData> => {
        const res = await fetch(endpointUrl as string, {
            method: 'POST',
            ...fetchParams,
            body: JSON.stringify({query: query.toString(), variables}),
        });

        const json = await res.json();

        if (json.errors) {
            const {message} = json.errors[0];

            throw new Error(message);
        }

        return json.data;
    };
}
