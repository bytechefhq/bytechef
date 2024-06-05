import {Alert, AlertDescription} from '@/components/ui/alert';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {ReactNode} from 'react';
import {Navigate, useLocation} from 'react-router-dom';

interface IOwnProps {
    hasAnyAuthorities?: string[];
    children: ReactNode;
}

const PrivateRoute = ({children, hasAnyAuthorities = [], ...rest}: IOwnProps) => {
    const {account, authenticated, sessionHasBeenFetched} = useAuthenticationStore();

    const isAuthorized = hasAnyAuthority(account?.authorities || [], hasAnyAuthorities);
    const pageLocation = useLocation();

    if (!children) {
        /* eslint-disable @typescript-eslint/no-explicit-any */
        throw new Error(`A component needs to be specified for private route for path ${(rest as any).path}`);
    }

    if (!sessionHasBeenFetched) {
        return <div></div>;
    }

    if (authenticated) {
        if (isAuthorized) {
            return <>{children}</>;
        }

        return (
            <div className="mx-auto w-96">
                <Alert className="m-4" variant="destructive">
                    <AlertDescription>You are not authorized to access this page.</AlertDescription>
                </Alert>
            </div>
        );
    }

    return (
        <Navigate
            replace
            state={{from: pageLocation}}
            to={{
                pathname: '/login',
                search: pageLocation.search,
            }}
        />
    );
};

export const hasAnyAuthority = (authorities: string[], hasAnyAuthorities: string[]) => {
    if (authorities && authorities.length !== 0) {
        if (hasAnyAuthorities.length === 0) {
            return true;
        }

        return hasAnyAuthorities.some((auth) => authorities.includes(auth));
    }
    return false;
};

/**
 * Checks authentication before showing the children and redirects to the
 * login page if the user is not authenticated.
 * If hasAnyAuthorities is provided the authorization status is also
 * checked and an error message is shown if the user is not authorized.
 */
export default PrivateRoute;
