<template>
  <div class="div">
    <div class="div-2">
      <div class="div-3">{{ workflow?.label }}</div>
      <div class="div-4">{{ workflow?.description }}</div>
    </div>
    <div class="div-5"><SwitchButton></SwitchButton></div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import { Workflow } from "../../../middleware/model/Workflow";
import SwitchButton from "./switch-button.vue";

interface WorkflowsItemProps {
  workflow?: Workflow;
}

export default defineComponent({
  name: "workflows-item",
  components: { SwitchButton: SwitchButton },
  props: ["workflow"],
});
</script>

<style scoped>
.div {
  display: flex;
  padding-top: 0.8rem;
  padding-bottom: 0.8rem;
  align-items: center;
}
.div:first-child {
  padding-top: 0;
}
.div:last-child {
  padding-top: 0;
}
.div-2 {
  display: flex;
  flex-direction: column;
  justify-content: between;
  flex-grow: 1;
}
.div-3 {
  font-size: 0.9rem;
  padding-bottom: 0.3rem;
}
.div-4 {
  color: #737c86;
  font-size: 0.9rem;
  line-height: 1.3;
}
.div-5 {
  display: flex;
  justify-content: end;
  margin-left: 1rem;
}
</style>