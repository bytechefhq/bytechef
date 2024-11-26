<template>
  <div class="div">
    <span>TO-DO list:</span>
    <div class="div-2">
      <input
        placeholder="Add a new item"
        class="input"
        :value="newItemName"
        @change="async (event) => (newItemName = event.target.value)"
      /><button class="button" @click="async (event) => addItem()">Add</button>
    </div>
    <div class="div-3">
      <ul class="ul">
        <template :key="idx" v-for="(item, idx) in list">
          <li class="li">
            <span>{{ item }}</span
            ><button
              class="button-2"
              @click="
                async (event) => {
                  deleteItem(idx);
                }
              "
            >
              Delete
            </button>
          </li>
        </template>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "todo-app",

  data() {
    return { list: ["hello", "world"], newItemName: "" };
  },

  methods: {
    addItem() {
      if (!this.newItemName) {
        return;
      }
      this.list = [...this.list, this.newItemName];
    },
    deleteItem(idx: number) {
      this.list = this.list.filter((x, i) => i !== idx);
    },
  },
});
</script>

<style scoped>
.div {
  padding: 10px;
  max-width: 700px;
}
.div-2 {
  display: flex;
  width: 100%;
  gap: 16px;
  align-items: stretch;
}
.input {
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  padding-left: 1rem;
  padding-right: 1rem;
  border-radius: 0.25rem;
  flex-grow: 1;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
.button {
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  padding-left: 1rem;
  padding-right: 1rem;
  border-radius: 0.25rem;
  font-weight: 700;
  color: #ffffff;
  background-color: #3b82f6;
  cursor: pointer;
}
.div-3 {
  margin-top: 1rem;
}
.ul {
  border-radius: 0.25rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
  margin: unset;
  padding: unset;
}
.li {
  display: flex;
  padding: 0.625rem;
  align-items: center;
  border-bottom-width: 1px;
  border-color: #e5e7eb;
  gap: 16px;
}
.button-2 {
  cursor: pointer;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  padding-left: 1rem;
  padding-right: 1rem;
  border-radius: 0.25rem;
  color: #ffffff;
  background-color: #ef4444;
}
</style>