interface ShouldShowAutomationGettingStartedHelpLinkProps {
    canCreateProject: boolean;
    development: boolean;
    pathname: string;
}

export default function shouldShowAutomationGettingStartedHelpLink({
    canCreateProject,
    development,
    pathname,
}: ShouldShowAutomationGettingStartedHelpLinkProps): boolean {
    return canCreateProject && development && pathname.startsWith('/automation/');
}
