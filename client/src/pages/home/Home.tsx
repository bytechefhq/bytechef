import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import * as Dialog from '@radix-ui/react-dialog';
import {FolderIcon, SquareIcon} from 'lucide-react';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';

const Home = () => {
    const {currentType, setCurrentType} = useModeTypeStore();

    const ff_520 = useFeatureFlagsStore()('ff-520');

    const navigate = useNavigate();

    const handleClick = (modeType: ModeType) => {
        setCurrentType(modeType);
    };

    useEffect(() => {
        if (!ff_520) {
            navigate('/automation');

            return;
        }

        if (currentType !== undefined) {
            if (currentType === ModeType.AUTOMATION) {
                navigate('/automation');
            } else {
                navigate('/embedded');
            }
        }
    }, [currentType, navigate, ff_520]);

    return ff_520 ? (
        <Dialog.Root open={currentType === undefined}>
            <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 z-50 bg-black/80" />

                <Dialog.Content className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 bg-background p-4 shadow-lg sm:rounded-lg">
                    <div className="mx-auto flex h-full items-center">
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <button onClick={() => handleClick(ModeType.EMBEDDED)}>
                                    <div className="flex size-80 flex-col items-center justify-between rounded-md p-4 hover:bg-green-100 hover:text-accent-foreground">
                                        <div className="text-2xl font-semibold">Embedded</div>

                                        <div>
                                            <SquareIcon className="size-16 text-green-300/70" />
                                        </div>

                                        <div>
                                            Allow your users to integrate your product with applications they use.
                                        </div>
                                    </div>
                                </button>
                            </div>

                            <div>
                                <button onClick={() => handleClick(ModeType.AUTOMATION)}>
                                    <div className="flex size-80 flex-col items-center justify-between rounded-md p-4 hover:bg-blue-100 hover:text-accent-foreground">
                                        <div className="text-2xl font-semibold">Automation</div>

                                        <div>
                                            <FolderIcon className="size-16 text-blue-300/70" />
                                        </div>

                                        <div>
                                            Integrate applications and automate processes inside your organization.
                                        </div>
                                    </div>
                                </button>
                            </div>
                        </div>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    ) : (
        <></>
    );
};

export default Home;
