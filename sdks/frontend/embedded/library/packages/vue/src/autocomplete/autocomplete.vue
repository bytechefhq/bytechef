<template>
  <div class="div">
    Autocomplete:
    <div class="div-2">
      <input
        placeholder="Search for a U.S. university"
        class="input"
        :value="inputVal"
        @change="async (event) => (inputVal = event.target.value)"
        @focus="async (event) => (showSuggestions = true)"
      /><button
        class="button"
        @click="
          async (event) => {
            inputVal = '';
            showSuggestions = false;
          }
        "
      >
        X
      </button>
    </div>
    <template v-if="suggestions.length > 0 && showSuggestions">
      <ul class="ul">
        <template :key="idx" v-for="(item, idx) in suggestions">
          <li class="li" @click="async (event) => handleClick(item)">
            <template v-if="renderChild">
              <component :item="item" :is="renderChild"></component>
            </template>

            <template v-else>
              <span>{{ transform(item) }}</span>
            </template>
          </li>
        </template>
      </ul>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export type Props = {
  getValues?: (input: string) => Promise<any[]>;
  renderChild?: any;
  transformData?: (item) => string;
};

export default defineComponent({
  name: "auto-complete",

  props: ["getValues", "transformData", "renderChild"],

  data() {
    return { showSuggestions: false, suggestions: [], inputVal: "" };
  },

  watch: {
    onUpdateHook0: {
      handler() {
        this.fetchVals(this.inputVal).then((newVals) => {
          if (!newVals?.filter) {
            console.error("Invalid response from getValues:", newVals);
            return;
          }
          this.suggestions = newVals.filter((data) =>
            this.transform(data)
              .toLowerCase()
              .includes(this.inputVal.toLowerCase())
          );
        });
      },
      immediate: true,
    },
  },

  computed: {
    onUpdateHook0() {
      return {
        0: this.inputVal,
        1: this.getValues,
      };
    },
  },

  methods: {
    setInputValue(value: string) {
      this.inputVal = value;
    },
    handleClick(item) {
      this.setInputValue(this.transform(item));
      this.showSuggestions = false;
    },
    fetchVals(city: string) {
      if (this.getValues) {
        return this.getValues(city);
      }
      return fetch(
        `http://universities.hipolabs.com/search?name=${city}&country=united+states`
      ).then((x) => x.json());
    },
    transform(x) {
      return this.transformData ? this.transformData(x) : x.name;
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
  position: relative;
  display: flex;
  gap: 16px;
  align-items: stretch;
}
.input {
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  padding-left: 1rem;
  padding-right: 1rem;
  border-radius: 0.25rem;
  border-width: 1px;
  border-color: #000000;
  width: 100%;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
.button {
  cursor: pointer;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  padding-left: 1rem;
  padding-right: 1rem;
  border-radius: 0.25rem;
  color: #ffffff;
  background-color: #ef4444;
}
.ul {
  border-radius: 0.25rem;
  height: 10rem;
  margin: unset;
  padding: unset;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
}
.li {
  display: flex;
  padding: 0.5rem;
  align-items: center;
  border-bottom-width: 1px;
  border-color: #e5e7eb;
  cursor: pointer;
}
.li:hover {
  background-color: #f3f4f6;
}
</style>