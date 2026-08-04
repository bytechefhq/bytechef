import reactLogo from '@/assets/logo.svg';
import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarRail,
    useSidebar,
} from '@/components/ui/sidebar';
import {type LucideIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {Link, useLocation} from 'react-router-dom';

import AppSidebarCollapsedGroup from './AppSidebarCollapsedGroup';
import {AppSidebarFooter} from './AppSidebarFooter';

export interface AppSidebarNavItemI {
    group?: string;
    href: string;
    icon: LucideIcon;
    name: string;
}

interface AppSidebarNavSectionI {
    items: AppSidebarNavItemI[];
    label?: string;
}

interface AppSidebarProps {
    navigation: AppSidebarNavItemI[];
}

export function AppSidebar({navigation}: AppSidebarProps) {
    const {pathname} = useLocation();

    const [openGroupLabel, setOpenGroupLabel] = useState<string | null>(null);

    const {isMobile, state} = useSidebar();

    // The mobile sidebar renders as a full sheet, never as an icon rail, so it keeps the expanded nav.
    const collapsed = state === 'collapsed' && !isMobile;

    const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

    // One flyout at a time. A group's close arrives on a delay, so by the time it lands the pointer may
    // already have opened the next group — clear the label only if it is still the one showing, otherwise
    // the outgoing group would close the incoming one.
    const handleGroupOpenChange = useCallback((label: string, open: boolean) => {
        setOpenGroupLabel((currentLabel) => {
            if (open) {
                return label;
            }

            return currentLabel === label ? null : currentLabel;
        });
    }, []);

    // Fold the flat navigation into ordered sections: consecutive items sharing a `group` render inside
    // one labeled SidebarGroup at the position of their first item; ungrouped runs render unlabeled.
    const sections = useMemo(() => {
        const foldedSections: AppSidebarNavSectionI[] = [];

        for (const item of navigation) {
            const lastSection = foldedSections[foldedSections.length - 1];

            if (lastSection && lastSection.label === item.group) {
                lastSection.items.push(item);
            } else {
                foldedSections.push({items: [item], label: item.group});
            }
        }

        return foldedSections;
    }, [navigation]);

    return (
        <Sidebar className="h-full bg-muted" collapsible="icon">
            <SidebarHeader>
                <Link className="flex items-center gap-2 py-1" to="/">
                    <span className="flex size-10 shrink-0 items-center justify-center">
                        <img alt="ByteChef" className="size-8 max-w-none shrink-0" src={reactLogo} />
                    </span>

                    <span className="text-lg font-semibold group-data-[collapsible=icon]:hidden">ByteChef</span>
                </Link>
            </SidebarHeader>

            <SidebarContent>
                <nav aria-label="Main navigation">
                    {sections.map((section, sectionIndex) => (
                        // py-1 (over the component's p-2) halves the vertical gap between stacked
                        // groups, and the shorter h-6 label buys the rest — the grouped nav otherwise
                        // outgrows shorter viewports.
                        <SidebarGroup className="py-1" key={`${section.label || 'main'}-${sectionIndex}`}>
                            {section.label && <SidebarGroupLabel className="h-6">{section.label}</SidebarGroupLabel>}

                            <SidebarGroupContent>
                                <SidebarMenu>
                                    {/* Collapsed: a labeled group folds into ONE hover-flyout icon; ungrouped
                                        items (AI Hub, Approval Tasks) and single-item groups (a flyout
                                        holding one link is pure friction) keep their own rail icon. */}

                                    {collapsed && section.label && section.items.length > 1 ? (
                                        <AppSidebarCollapsedGroup
                                            isActive={isActive}
                                            items={section.items}
                                            label={section.label}
                                            onOpenChange={handleGroupOpenChange}
                                            open={openGroupLabel === section.label}
                                        />
                                    ) : (
                                        section.items.map((item) => (
                                            <SidebarMenuItem key={item.name}>
                                                <SidebarMenuButton
                                                    asChild
                                                    className="h-10 gap-3 text-sm group-data-[collapsible=icon]:!size-10 data-[active=true]:font-medium data-[active=true]:text-content-brand-primary [&>svg]:size-6"
                                                    isActive={isActive(item.href)}
                                                    tooltip={item.name}
                                                >
                                                    <Link to={item.href}>
                                                        <item.icon aria-hidden="true" />

                                                        <span>{item.name}</span>
                                                    </Link>
                                                </SidebarMenuButton>
                                            </SidebarMenuItem>
                                        ))
                                    )}
                                </SidebarMenu>
                            </SidebarGroupContent>
                        </SidebarGroup>
                    ))}
                </nav>
            </SidebarContent>

            <SidebarFooter>
                <AppSidebarFooter />
            </SidebarFooter>

            <SidebarRail />
        </Sidebar>
    );
}
