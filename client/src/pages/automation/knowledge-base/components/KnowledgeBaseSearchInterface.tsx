import Button from '@/components/Button/Button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import useKnowledgeBaseSearchInterface from '@/pages/automation/knowledge-base/components/hooks/useKnowledgeBaseSearchInterface';
import {SearchIcon} from 'lucide-react';
import {useMemo} from 'react';

interface KnowledgeBaseSearchInterfaceProps {
    knowledgeBaseId: string;
}

const KnowledgeBaseSearchInterface = ({knowledgeBaseId}: KnowledgeBaseSearchInterfaceProps) => {
    const {
        canSearch,
        handleClearSearch,
        handleSearch,
        isLoading,
        metadataFilters,
        query,
        results,
        searchQuery,
        setMetadataFilters,
        setQuery,
    } = useKnowledgeBaseSearchInterface({knowledgeBaseId});

    const resultsWithChunkIndex = useMemo(() => {
        const documentChunkCounts: Record<string, number> = {};

        return results.map((result, index) => {
            const documentName = String((result.metadata as Record<string, unknown>)?.file_name ?? 'Unknown');

            documentChunkCounts[documentName] = (documentChunkCounts[documentName] ?? 0) + 1;

            return {
                chunkIndex: documentChunkCounts[documentName],
                documentName,
                index,
                result,
            };
        });
    }, [results]);

    const renderSearchResults = () => {
        if (results.length === 0) {
            return (
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-8 text-center">
                    <p className="text-gray-500">No results found for your query.</p>
                </div>
            );
        }

        return resultsWithChunkIndex.map(({chunkIndex, documentName, index, result}) => (
            <div
                className="rounded-lg border border-gray-200 bg-white p-4 transition-shadow hover:shadow-md"
                key={result.id}
            >
                <div className="mb-2 flex items-center justify-between">
                    <div className="flex items-center space-x-2 text-sm text-gray-500">
                        <span className="font-medium">Result {index + 1}</span>

                        <span>•</span>

                        <span className="font-medium">{documentName}</span>

                        <span>•</span>

                        <span>Chunk {chunkIndex}</span>

                        {result.score != null && (
                            <>
                                <span>•</span>

                                <span className="font-medium text-blue-600">
                                    Score: {(result.score * 100).toFixed(1)}%
                                </span>
                            </>
                        )}
                    </div>
                </div>

                <p className="text-sm leading-relaxed text-gray-700">{result.content}</p>

                {result.metadata && (
                    <div className="mt-3 rounded-md bg-gray-50 p-2 text-xs text-gray-600">
                        <span className="font-medium">Metadata: </span>

                        <pre className="mt-1 overflow-x-auto">
                            <code>{JSON.stringify(result.metadata, null, 2)}</code>
                        </pre>
                    </div>
                )}
            </div>
        ));
    };

    return (
        <div className="space-y-4">
            <form className="space-y-4 rounded-lg border border-gray-200 bg-white p-4" onSubmit={handleSearch}>
                <fieldset className="space-y-4 border-0">
                    <div className="space-y-2">
                        <Label htmlFor="query">Search Query</Label>

                        <div className="flex space-x-2">
                            <Input
                                id="query"
                                onChange={(event) => setQuery(event.target.value)}
                                placeholder="Enter your search query..."
                                value={query}
                            />

                            <Button disabled={isLoading || !canSearch} type="submit">
                                <SearchIcon className="mr-2 size-4" />

                                {isLoading ? 'Searching...' : 'Search'}
                            </Button>
                        </div>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="filters">Metadata Filters (JSON, optional)</Label>

                        <Input
                            id="filters"
                            onChange={(event) => setMetadataFilters(event.target.value)}
                            placeholder='{"category": "documentation"}'
                            value={metadataFilters}
                        />
                    </div>
                </fieldset>

                {searchQuery && (
                    <div className="flex items-center justify-between border-t border-gray-200 pt-4">
                        <p className="text-sm text-gray-500">
                            Found {results.length} result{results.length !== 1 ? 's' : ''}
                        </p>

                        <Button onClick={handleClearSearch} size="sm" variant="outline">
                            Clear Search
                        </Button>
                    </div>
                )}
            </form>

            {searchQuery && <div className="space-y-2">{renderSearchResults()}</div>}
        </div>
    );
};

export default KnowledgeBaseSearchInterface;
