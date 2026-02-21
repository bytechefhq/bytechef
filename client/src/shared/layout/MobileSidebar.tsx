import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {Dialog, DialogContent} from '@/components/ui/dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AudioLinesIcon, User2Icon} from 'lucide-react';
import {ForwardRefExoticComponent, SVGProps} from 'react';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import reactLogo from '../../assets/logo.svg';

interface MobileSidebarProps {
    navigation: {
        name: string;
        href: string;
        icon: ForwardRefExoticComponent<Omit<SVGProps<SVGSVGElement>, 'ref'>>;
    }[];
    mobileMenuOpen: boolean;
    setMobileMenuOpen: (value: boolean) => void;
}

export function MobileSidebar({mobileMenuOpen, navigation, setMobileMenuOpen}: MobileSidebarProps) {
    const account = useAuthenticationStore((state) => state.account);
    const edition = useApplicationInfoStore((state) => state.application?.edition);
    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );
    const currentType = usePlatformTypeStore((state) => state.currentType);

    const navigate = useNavigate();

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: environmentsQuery} = useEnvironmentsQuery();

    const handleEnvironmentValueChange = (value: string) => {
        setCurrentEnvironmentId(+value);

        if (currentType === PlatformType.AUTOMATION) {
            navigate(`/automation${+value === DEVELOPMENT_ENVIRONMENT ? '/projects' : '/deployments'}`);
        } else if (currentType === PlatformType.EMBEDDED) {
            navigate(`/embedded${+value === DEVELOPMENT_ENVIRONMENT ? '/integrations' : '/configurations'}`);
        }
    };

    return (
        <Dialog onOpenChange={setMobileMenuOpen} open={mobileMenuOpen}>
            <DialogContent className="flex h-full flex-col bg-white p-0 focus:outline-none">
                <div className="pb-4 pt-5">
                    <div className="flex shrink-0 items-center px-4">
                        <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                    </div>

                    <nav aria-label="Sidebar" className="mt-5">
                        <div className="space-y-1 px-2">
                            {navigation.map((item) => (
                                <a
                                    className="group flex items-center rounded-md p-2 text-base font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900"
                                    href={item.href}
                                    key={item.name}
                                >
                                    <item.icon
                                        aria-hidden="true"
                                        className="mr-4 size-6 text-gray-400 group-hover:text-gray-500"
                                    />

                                    {item.name}
                                </a>
                            ))}
                        </div>
                    </nav>
                </div>

                {edition === 'EE' && environmentsQuery?.environments && (
                    <div className="border-t border-gray-200 px-4 py-3">
                        <div className="flex items-center space-x-2 pb-2">
                            <AudioLinesIcon className="size-5 text-gray-500" />

                            <span className="text-sm font-semibold text-gray-700">Environment</span>
                        </div>

                        <Select onValueChange={handleEnvironmentValueChange} value={currentEnvironmentId?.toString()}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Select environment" />
                            </SelectTrigger>

                            <SelectContent>
                                {environmentsQuery.environments.map((environment) => (
                                    <SelectItem key={environment?.id} value={environment?.id!}>
                                        {environment?.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                )}

                <div className="mt-auto flex shrink-0 items-center space-x-3 border-t border-gray-200 p-4">
                    <Avatar>
                        <AvatarFallback className="bg-muted">
                            <User2Icon className="size-6" />
                        </AvatarFallback>
                    </Avatar>

                    <div>
                        <p className="text-sm text-muted-foreground">Signed in as</p>

                        <p className="text-sm font-medium text-gray-700">{account?.email}</p>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
