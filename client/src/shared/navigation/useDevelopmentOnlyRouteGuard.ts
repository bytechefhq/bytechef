import {getDevelopmentOnlyFallbackHref} from '@/shared/navigation/developmentOnlyRoutes';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

export function useDevelopmentOnlyRouteGuard(): void {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {pathname} = useLocation();

    const navigate = useNavigate();

    useEffect(() => {
        const fallbackHref = getDevelopmentOnlyFallbackHref(pathname, currentEnvironmentId);

        if (fallbackHref) {
            navigate(fallbackHref, {replace: true});
        }
    }, [currentEnvironmentId, navigate, pathname]);
}
