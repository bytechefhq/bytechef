import Button from '@/components/Button/Button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubTabsStore, attachTab} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useGetAssetFilesQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {FilePlusIcon, FileTextIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useDebouncedCallback} from 'use-debounce';

const SECTION_INITIAL_CAP = 20;
const SECTION_EXPAND_INCREMENT = 50;

const AiHubFilePicker = () => {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [filesShowCount, setFilesShowCount] = useState(SECTION_INITIAL_CAP);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: assetFilesData, isLoading} = useGetAssetFilesQuery(
        {environment: currentEnvironmentId, workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: open && isWorkspaceIdKnown(currentWorkspaceId)}
    );

    const handleSearchChange = useDebouncedCallback((value: string) => {
        setDebouncedSearch(value);
    }, 300);

    const filteredFiles = useMemo(() => {
        const lowerSearch = debouncedSearch.toLowerCase();

        return (assetFilesData?.assetFiles ?? []).filter((file) => file.name.toLowerCase().includes(lowerSearch));
    }, [assetFilesData, debouncedSearch]);

    const visibleFiles = filteredFiles.slice(0, filesShowCount);

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);

        if (!nextOpen) {
            setSearch('');
            setDebouncedSearch('');
            setFilesShowCount(SECTION_INITIAL_CAP);
        }
    };

    const handleSelectFile = (fileId: string, name: string) => {
        const {ownsTab, tabId} = attachTab(() => aiHubTabsStore.getState().openFileTab(fileId, name));

        aiHubComposerStore.getState().addReference({id: fileId, kind: 'file', name, ownsTab, tabId});

        handleOpenChange(false);
    };

    return (
        <Popover onOpenChange={handleOpenChange} open={open}>
            <Tooltip>
                <TooltipTrigger asChild>
                    <PopoverTrigger asChild>
                        <Button aria-label="Open file" icon={<FilePlusIcon />} size="icon" variant="ghost" />
                    </PopoverTrigger>
                </TooltipTrigger>

                <TooltipContent>Open file</TooltipContent>
            </Tooltip>

            <PopoverContent align="end" className="w-72 p-0">
                <Command shouldFilter={false}>
                    <div className="mt-1 pb-3">
                        <CommandInput
                            className="!border-0 !ring-0 !outline-none focus:!border-0 focus:!ring-0 focus:!outline-none"
                            onValueChange={(value) => {
                                setSearch(value);
                                handleSearchChange(value);
                            }}
                            placeholder="Search files…"
                            value={search}
                        />
                    </div>

                    <CommandList>
                        {filteredFiles.length === 0 && (
                            <CommandEmpty>{isLoading ? 'Loading…' : 'No files.'}</CommandEmpty>
                        )}

                        {filteredFiles.length > 0 && (
                            <CommandGroup heading="Files">
                                {visibleFiles.map((file) => (
                                    <CommandItem
                                        key={`file-${file.id}`}
                                        onSelect={() => handleSelectFile(file.id, file.name)}
                                        value={`file-${file.id}-${file.name}`}
                                    >
                                        <FileTextIcon className="mr-2 size-3.5" />

                                        <span className="truncate">{file.name}</span>
                                    </CommandItem>
                                ))}

                                {filteredFiles.length > filesShowCount && (
                                    <CommandItem
                                        key="files-show-more"
                                        onSelect={() => setFilesShowCount((count) => count + SECTION_EXPAND_INCREMENT)}
                                        value="files-show-more"
                                    >
                                        <span className="text-xs text-muted-foreground">
                                            {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredFiles.length - filesShowCount)} more…`}
                                        </span>
                                    </CommandItem>
                                )}
                            </CommandGroup>
                        )}
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
};

export default AiHubFilePicker;
