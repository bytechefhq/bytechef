<template>
  <div class="div">
    <nav class="nav">
      <ul role="tablist" aria-orientation="horizontal" class="ul">
        <template :key="`tab-${index}`" v-for="(tab, index) in tabs">
          <li class="li">
            <TabsListButton
              :activeTab="activeTab"
              :id="tab.id"
              :label="tab.label"
              :onClick="(tabId) => (activeTab = tabId)"
            ></TabsListButton>
          </li>
        </template>
      </ul>
    </nav>
    <slot />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import Context from "./tabs-list.context";
import TabsListButton from "./tabs-list-button.vue";

export interface TabsListProps {
  activeTab: string;
  tabs: {
    id: string;
    label: string;
  }[];
  children: any;
}

export default defineComponent({
  name: "tabs-list",
  components: { TabsListButton: TabsListButton },
  props: ["activeTab", "tabs"],

  data() {
    return { activeTab: this.activeTab };
  },

  provide() {
    const _this = this;
    return {
      [Context.key]: { activeTab: _this.activeTab },
    };
  },
});
</script>

<style scoped>
.div {
  width: 100%;
}
.nav {
  overflow-x: scroll;
  scrollbar-width: none;
  overflow-scrolling: touch;
  -webkit-overflow-scrolling: touch;
}
.ul {
  width: fit-content;
  min-width: 100%;
  color: #57606f;
  display: flex;
  gap: 0.5em;
  border-bottom: 2px solid #ddd;
  margin: 0;
  padding: 0;
}
.li {
  display: block;
  margin-bottom: -2px;
}
</style>