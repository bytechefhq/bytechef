import Badge from '@/components/Badge/Badge';
import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectionListItemDropdownMenu from '@/shared/components/connection/ConnectionListItemDropdownMenu';
import {ComponentIcon} from 'lucide-react';
import {ReactNode} from 'react';

interface ConnectionListItemLayoutProps {
    active?: boolean;
    badges?: ReactNode;
    children?: ReactNode;
    createdDate?: Date;
    credentialStatus?: string;
    icon?: string;
    id?: number;
    menuItems: ReactNode;
    name: string;
    onEditClick: () => void;
    tagList: ReactNode;
}

const ConnectionListItemLayout = ({
    active,
    badges,
    children,
    createdDate,
    credentialStatus,
    icon,
    id,
    menuItems,
    name,
    onEditClick,
    tagList,
}: ConnectionListItemLayoutProps) => (
    <li className="mb-2 rounded border border-border/50" key={id}>
        <div className="group flex items-center rounded-md bg-surface-neutral-primary px-3 hover:bg-surface-neutral-primary-hover">
            <div className="flex flex-1 items-center py-3">
                <div className="flex-1">
                    <div className="flex min-h-8 items-center justify-between">
                        <div className="relative flex items-center gap-2">
                            {icon ? (
                                <LazyLoadSVG className="size-5 flex-none" preloader={<ComponentIcon />} src={icon} />
                            ) : (
                                <ComponentIcon className="size-5 flex-none" />
                            )}

                            <span className="text-base font-semibold">{name}</span>
                        </div>
                    </div>

                    <div className="mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">{tagList}</div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6 [&>:last-child]:-ml-3">
                    <div className="flex min-w-52 flex-col items-end gap-y-2">
                        <div className="flex min-h-8 flex-wrap items-center justify-end gap-2">
                            {credentialStatus === 'VALID' ? (
                                <Badge
                                    className="border-transparent uppercase"
                                    label={active ? 'Active' : 'Not Active'}
                                    styleType={active ? 'success-outline' : 'secondary-outline'}
                                    weight="semibold"
                                />
                            ) : (
                                <Badge
                                    className="border-transparent uppercase"
                                    label={credentialStatus ?? 'INVALID'}
                                    styleType="destructive-outline"
                                    weight="semibold"
                                />
                            )}

                            {badges}
                        </div>

                        {createdDate && (
                            <Tooltip>
                                <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary sm:mt-0">
                                    <span className="text-xs">
                                        {`Created at ${createdDate.toLocaleDateString()} ${createdDate.toLocaleTimeString()}`}
                                    </span>
                                </TooltipTrigger>

                                <TooltipContent>Created Date</TooltipContent>
                            </Tooltip>
                        )}
                    </div>

                    <ConnectionListItemDropdownMenu onEditClick={onEditClick}>
                        {menuItems}
                    </ConnectionListItemDropdownMenu>
                </div>
            </div>
        </div>

        {children}
    </li>
);

export default ConnectionListItemLayout;
