import {FolderIcon, SquareIcon} from 'lucide-react';
import {Link} from 'react-router-dom';

const Home = () => {
    return (
        <>
            <div className="mx-auto flex h-full items-center">
                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <Link to="/embedded">
                            <div className="flex size-64 flex-col items-center justify-between rounded-md bg-green-100 p-4 hover:bg-green-200 hover:text-accent-foreground">
                                <div className="font-bold">Embedded</div>

                                <div>
                                    <SquareIcon className="size-16 text-green-300/70" />
                                </div>

                                <div>Allow your users to integrate your product with applications they use.</div>
                            </div>
                        </Link>
                    </div>

                    <div>
                        <Link to="/automation">
                            <div className="flex size-64 flex-col items-center justify-between rounded-md bg-blue-100 p-4 hover:bg-blue-200 hover:text-accent-foreground">
                                <div className="font-bold">Automation</div>

                                <div>
                                    <FolderIcon className="size-16 text-blue-300/70" />
                                </div>

                                <div>Integrate applications and automate processes inside your organization.</div>
                            </div>
                        </Link>
                    </div>
                </div>
            </div>
        </>
    );
};

export default Home;
