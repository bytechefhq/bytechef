const LEGACY_EMBEDDED_WORKFLOW_BUILDER_PATH = /^\/embedded\/workflow-builder\/([^/]+)\/?$/;

export const getLegacyEmbeddedWorkflowBuilderUrl = ({
    pathname,
    search,
}: {
    pathname: string;
    search: string;
}): string | undefined => {
    const match = LEGACY_EMBEDDED_WORKFLOW_BUILDER_PATH.exec(pathname);

    if (!match) {
        return undefined;
    }

    return `/workflow-builder.html${search}#/embedded/builder/${match[1]}`;
};
