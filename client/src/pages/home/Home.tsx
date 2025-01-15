import {useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

import ModeSelectionDialog from './ModeSelectionDialog';

const Home = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const {currentType} = useModeTypeStore();

    const navigate = useNavigate();

    const ff_520 = useFeatureFlagsStore()('ff-520');

    useEffect(() => {
        if (!ff_520) {
            navigate('/automation');

            return;
        }

        if (currentType === undefined) {
            setIsDialogOpen(true);
        }
    }, [currentType, navigate, ff_520]);

    return (
        <>
            {ff_520 && isDialogOpen && (
                <ModeSelectionDialog isDialogOpen={isDialogOpen} onDialogClose={() => setIsDialogOpen(false)} />
            )}
        </>
    );
};

export default Home;
