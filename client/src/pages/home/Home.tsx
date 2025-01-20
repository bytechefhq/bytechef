import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import ModeSelectionDialog from './ModeSelectionDialog';

const Home = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const {currentType} = useModeTypeStore();

    const navigate = useNavigate();

    const ff_520 = useFeatureFlagsStore()('ff-520');

    const memoizedNavigate = useCallback(() => navigate('/automation'), [navigate]);

    useEffect(() => {
        if (!ff_520) {
            memoizedNavigate();

            return;
        }

        if (currentType !== undefined) {
            if (currentType === ModeType.AUTOMATION) {
                navigate('/automation');
            } else if (currentType === ModeType.EMBEDDED) {
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
