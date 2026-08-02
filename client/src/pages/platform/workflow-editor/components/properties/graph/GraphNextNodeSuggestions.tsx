import Button from '@/components/Button/Button';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';

interface GraphNextNodeSuggestionsPropsI {
    disabled?: boolean;
    nodeNames: Array<string>;
    path: string;
}

/**
 * One-click completions for a graph node's `next` expression, offering every declared node name
 * (INCLUDING the node's own name — self-loops are legal, so a node is always a valid target for
 * its own `next`).
 *
 * This is deliberately an ADJACENT affordance rather than a threaded-in completion source for
 * `PropertyMentionsInput`'s `$`-triggered mention popup: that popup inserts `${...}` data-pill
 * references via a Tiptap `Mention` node, which is the wrong shape for a plain quoted string
 * literal. Reusing it here would mean teaching the mention system a second, incompatible
 * insertion kind for the sake of one property. Keeping this scoped to `GraphStatesPanel` (the
 * only caller) means no other property's mention/data-pill behavior is touched at all.
 *
 * Picking a name writes the quoted literal (`'reviewed'`) as the node's entire `next` value
 * through the same `saveProperty` path the mentions editor itself debounces into. The `next`
 * `Property` editor is not focused when a suggestion button is clicked (focus is on the button),
 * so its existing "sync value prop when not focused" effect
 * (`PropertyMentionsInputEditor`) picks up the change and re-renders the quoted literal —
 * no direct Tiptap/editor-ref access is needed here. This replaces the whole expression rather
 * than inserting at a mid-text cursor position; for the common case (an empty or single-literal
 * `next`) that is indistinguishable from an insert, and it keeps this component free of any
 * editor internals.
 *
 * `includeInMetadata: true` deliberately mirrors `PropertyMentionsInputEditor`'s OWN debounced
 * save call for this exact property (`saveMentionInputValue`, which hardcodes `true`
 * unconditionally — unlike the `includeInMetadata: custom` pattern used by most OTHER save paths
 * in `useProperty.ts`). A chip click and a typed edit must write through the identical
 * `saveProperty` shape; matching `true` here is what achieves that, not `false`.
 *
 * `disabled` is the clobber guard: a chip click REPLACES the whole `next` value (see above), so
 * it must not be offered while `next` holds anything the caller (`GraphStatesPanel`, via
 * `extractNextTargets`'s `dynamic` flag) can't prove is a safely-replaceable bare literal — e.g.
 * a hand-written ternary. Disabled chips keep a `title` explaining why instead of the "set to"
 * hint. Deliberately still ENABLED for an empty `next` (nothing to lose) and for a dangling bare
 * literal (a stale reference after a rename — clicking is a one-click repair, not a clobber).
 */
export default function GraphNextNodeSuggestions({disabled = false, nodeNames, path}: GraphNextNodeSuggestionsPropsI) {
    const {updateClusterElementParameterMutation, updateWorkflowNodeParameterMutation} = useWorkflowEditor();
    const workflowId = useWorkflowDataStore((state) => state.workflow.id);

    if (!nodeNames.length) {
        return null;
    }

    const handleSuggestionClick = (nodeName: string) => {
        if (disabled || !workflowId) {
            return;
        }

        saveProperty({
            includeInMetadata: true,
            path,
            type: 'STRING',
            updateClusterElementParameterMutation,
            updateWorkflowNodeParameterMutation,
            value: `'${nodeName}'`,
            workflowId,
        });
    };

    return (
        <div aria-label="Next expression suggestions" className="flex flex-wrap items-center gap-1">
            {nodeNames.map((nodeName) => (
                <Button
                    disabled={disabled}
                    key={`next_suggestion_${nodeName}`}
                    label={nodeName}
                    onClick={() => handleSuggestionClick(nodeName)}
                    size="xxs"
                    title={disabled ? 'Clear the expression to use quick targets' : `Set next to '${nodeName}'`}
                    type="button"
                    variant="outline"
                />
            ))}
        </div>
    );
}
