<template>
  <div class="div">
    <template
      :key="workflow.workflowReferenceCode"
      v-for="(workflow, index) in workflows"
    >
      <WorkflowsItem :workflow="workflow"></WorkflowsItem>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import { Workflow } from "../../../middleware/model/Workflow";
import WorkflowsItem from "./workflows-item.vue";

interface WorkflowsListProps {
  workflows?: Workflow[];
}

export default defineComponent({
  name: "workflows-list",
  components: { WorkflowsItem: WorkflowsItem },
  props: ["workflows"],
});
</script>

<style scoped>
.div {
  overflow-y: auto;
  max-height: 600px;
}
</style>