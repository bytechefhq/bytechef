import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import ModeSelectionDialog from './components/ModeSelectionDialog';

const Home = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const currentType = usePlatformTypeStore((state) => state.currentType);

    const navigate = useNavigate();

    const ff_520 = useFeatureFlagsStore()('ff-520');

    const memoizedNavigate = useCallback(() => navigate('/automation'), [navigate]);

    useEffect(() => {
        if (!ff_520) {
            memoizedNavigate();

            return;
        }

        if (currentType !== undefined) {
            if (currentType === PlatformType.AUTOMATION) {
                navigate('/automation');
            } else if (currentType === PlatformType.EMBEDDED) {
                navigate('/embedded');
            }
        }

        if (currentType === undefined) {
            setIsDialogOpen(true);
        }
    }, [ff_520, currentType, memoizedNavigate, navigate]);

    if (!ff_520 || !isDialogOpen) {
        return <></>;
    }

    return <ModeSelectionDialog handleDialogClose={() => setIsDialogOpen(false)} isDialogOpen={isDialogOpen} />;
};

export default Home;
