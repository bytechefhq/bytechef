import {ReactNode} from 'react';
import {Navigate, useLocation, useSearchParams} from 'react-router-dom';

interface AccessControlProps {
    children: ReactNode;
    requiresFlow?: boolean;
    requiresKey?: boolean;
}

export const AccessControl = ({children, requiresFlow = false, requiresKey = false}: AccessControlProps) => {
    const [searchParams] = useSearchParams();
    const location = useLocation();
    const key = searchParams.get('key');

    const hasActivationKey = Boolean(key);

    const isFromInternalFlow = requiresFlow ? Boolean(location.state?.fromInternalFlow) : true;

    const canRenderWithKey = requiresKey && hasActivationKey;
    const canRenderWithFlow = requiresFlow && isFromInternalFlow;

    const shouldRedirectToLogin = !canRenderWithKey && !canRenderWithFlow;

    if (shouldRedirectToLogin) {
        return <Navigate replace to="/login" />;
    }

    return <>{children}</>;
};
