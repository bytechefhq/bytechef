<template>
  <div class="div">
    <div class="div-2">
      <CloseButton :onClose="(event) => onClose && onClose()"></CloseButton>
      <div class="div-3">
        <div class="div-4">
          <img
            :src="`data:image/svg+xml;utf8,${integration?.icon}`"
            :alt="integration?.title"
          />
        </div>
        <div class="div-5">{{ integration?.title }}</div>
      </div>
      <div class="div-6">
        <template v-if="integration">
          <template v-if="integration?.workflows.length">
            <TabsList
              activeTab="tab1"
              :tabs="[
                {
                  id: 'tab1',
                  label: 'Overview',
                },
                {
                  id: 'tab2',
                  label: 'Configuration',
                },
              ]"
              ><TabsItem id="tab1"
                ><div class="div-10">
                  {{ integration?.description }}
                </div></TabsItem
              ><TabsItem id="tab2"
                ><WorkflowsList
                  :workflows="integration?.workflows"
                ></WorkflowsList></TabsItem
            ></TabsList>
          </template>

          <template v-else>
            <div class="div-7">
              <div class="div-8">Overview</div>
              <div class="div-9">{{ integration?.description }}</div>
            </div>
          </template>
        </template>

        <template v-else> Loading... </template>
      </div>
      <ConnectButton></ConnectButton><PoweredBy></PoweredBy>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import CloseButton from "./components/close-button.vue";
import ConnectButton from "./components/connect-button.vue";
import TabsList from "./components/tabs/tabs-list.vue";
import TabsItem from "./components/tabs/tabs-item.vue";
import fetchIntegration from "../middleware/fetch-integration";
import { Integration } from "../middleware/model/Integration";
import WorkflowsList from "./components/workflows/workflows-list.vue";
import PoweredBy from "./components/powered-by.vue";

interface ConnectDialogProps {
  onClose?: () => void;
}

export default defineComponent({
  name: "connect-dialog",
  components: {
    CloseButton: CloseButton,
    TabsList: TabsList,
    TabsItem: TabsItem,
    WorkflowsList: WorkflowsList,
    ConnectButton: ConnectButton,
    PoweredBy: PoweredBy,
  },
  props: ["onClose"],

  data() {
    return { integration: undefined };
  },

  mounted() {
    fetchIntegration().then((response) => {
      console.log(response);
      this.integration = response;
    });
  },
});
</script>

<style scoped>
.div {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
}
.div-2 {
  max-width: 480px;
  min-height: 300px;
  margin: 150px auto;
  background-color: #ffff;
  border-radius: 6px;
  padding: 1rem;
  position: relative;
  display: flex;
  flex-direction: column;
  font-family: Arial, sans-serif;
}
.div-3 {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}
.div-4 {
  width: 24px;
}
.div-5 {
  font-size: 1.3rem;
  margin-left: 0.5rem;
}
.div-6 {
  display: flex;
  flex-grow: 1;
}
.div-7 {
  display: flex;
  flex-direction: column;
}
.div-8 {
  font-size: 1rem;
  margin-bottom: 0.5rem;
}
.div-9 {
  color: #737c86;
  font-size: 0.9rem;
  line-height: 1.3;
}
.div-10 {
  color: #737c86;
  font-size: 0.9rem;
  line-height: 1.3;
}
</style>