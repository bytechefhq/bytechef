<script context="module" lang="ts">
  interface WorkflowsListProps {
    workflows?: Workflow[];
  }
</script>

<script lang="ts">
  import { Workflow } from "../../../middleware/model/Workflow";
  import WorkflowsItem from "./workflows-item.svelte";

  export let workflows: WorkflowsListProps["workflows"] = undefined;
</script>

<div class="div">
  {#each workflows as workflow (workflow.workflowReferenceCode)}
    <WorkflowsItem {workflow} />
  {/each}
</div>

<style>
  .div {
    overflow-y: auto;
    max-height: 600px;
  }
</style>