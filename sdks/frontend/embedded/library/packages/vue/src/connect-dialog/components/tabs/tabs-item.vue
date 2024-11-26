<template>
  <template v-if="context.activeTab === id">
    <div
      role="tabpanel"
      class="div"
      :aria-labelledby="`tab-${id}`"
      :id="`panel-${id}`"
    >
      <slot />
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import Context from "./tabs-list.context";

export interface TabsItemProps {
  id: string;
  children: any;
}

export default defineComponent({
  name: "tabs-item",

  props: ["id"],

  inject: {
    context: Context.key,
  },
});
</script>

<style scoped>
.div {
  margin-top: 0.7rem;
  margin-bottom: 0.7rem;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  border-radius: 0.5rem;
}
</style>