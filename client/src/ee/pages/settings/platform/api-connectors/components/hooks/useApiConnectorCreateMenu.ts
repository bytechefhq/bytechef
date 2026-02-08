import {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';

import {useApiConnectorWizardStore} from '../../stores/useApiConnectorWizardStore';

interface UseApiConnectorCreateMenuI {
    handleMenuItemClick: (path: string) => void;
}

export default function useApiConnectorCreateMenu(): UseApiConnectorCreateMenuI {
    const reset = useApiConnectorWizardStore((state) => state.reset);
    const navigate = useNavigate();

    const handleMenuItemClick = useCallback(
        (path: string) => {
            reset();
            navigate(path);
        },
        [navigate, reset]
    );

    return {
        handleMenuItemClick,
    };
}
