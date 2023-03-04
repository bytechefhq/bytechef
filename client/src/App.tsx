import {useState} from 'react';
import {
    FolderPlusIcon,
    LightBulbIcon,
    LinkIcon,
    QueueListIcon,
} from '@heroicons/react/24/outline';
import {DesktopSidebar} from './components/Sidebar/DesktopSidebar';
import {MobileTopNavigation} from './components/Sidebar/MobileTopNavigation';
import {MobileSidebar} from './components/Sidebar/MobileSidebar';
import {Outlet} from 'react-router-dom';

const user = {
    name: 'Emily Selman',
    email: 'emily.selman@example.com',
    imageUrl:
        'https://images.unsplash.com/photo-1502685104226-ee32379fefbe?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80',
};
const navigation: {
    name: string;
    href: string;
    icon: React.ForwardRefExoticComponent<React.SVGProps<SVGSVGElement>>;
}[] = [
    {
        name: 'Projects',
        href: '/automation/projects',
        icon: FolderPlusIcon,
    },
    {name: 'Instances', href: '/automation/instances', icon: LightBulbIcon},
    {name: 'Connections', href: '/automation/connections', icon: LinkIcon},
    {name: 'Executions', href: '/automation/executions', icon: QueueListIcon},
];

function App() {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    return (
        <>
            <div className="flex h-full">
                <MobileSidebar
                    user={user}
                    navigation={navigation}
                    mobileMenuOpen={mobileMenuOpen}
                    setMobileMenuOpen={setMobileMenuOpen}
                />

                <DesktopSidebar navigation={navigation} />

                <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
                    <MobileTopNavigation
                        setMobileMenuOpen={setMobileMenuOpen}
                    />

                    <Outlet />
                </div>
            </div>
        </>
    );
}

export default App;
