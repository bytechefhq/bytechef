<template>
  <button
    role="tab"
    class="button"
    :key="`tab-btn-${id}`"
    :id="`tab-${id}`"
    :aria-controls="`panel-${id}`"
    :aria-selected="activeTab === id"
    @click="async (event) => onClick(id)"
    :style="{
      color: activeTab === id ? '#2563EB' : 'inherit',
      borderColor: activeTab === id ? '#2563EB' : 'transparent',
    }"
  >
    {{ label }}
  </button>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export interface TabsListButtonProps {
  activeTab: string;
  id: string;
  label: string;
  onClick: (tabId: string) => void;
}

export default defineComponent({
  name: "tabs-list-button",

  props: ["id", "activeTab", "onClick", "label"],
});
</script>

<style scoped>
.button {
  font-size: 0.9rem;
  padding: 0.75em 1em;
  background-color: transparent;
  border: 2px solid #ddd;
  border-width: 0 0 2px;
  cursor: pointer;
  white-space: nowrap;
}
</style>