import ModeSelectionDialog from '@/pages/home/ModeSelectionDialog';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useState} from 'react';

import reactLogo from '../../../assets/logo.svg';

const DesktopSidebarTopMenu = () => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const ff_520 = useFeatureFlagsStore()('ff-520');

    return (
        <>
            <button onClick={() => setIsDialogOpen(true)}>
                <img alt="ByteChef" className="h-8 w-auto cursor-pointer" src={reactLogo} />
            </button>

            {ff_520 && <ModeSelectionDialog isDialogOpen={isDialogOpen} onDialogClose={() => setIsDialogOpen(false)} />}
        </>
    );
};

export default DesktopSidebarTopMenu;
