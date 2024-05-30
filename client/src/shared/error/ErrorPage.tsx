import {useRouteError} from 'react-router-dom';

export default function ErrorPage() {
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    const error: any = useRouteError();

    return (
        <div className="space-y-2 p-4" id="error-page">
            <h1 className="text-lg">Oops!</h1>

            <p>Sorry, an unexpected error has occurred.</p>

            <p>
                <i>{error.statusText || error.message}</i>
            </p>
        </div>
    );
}
