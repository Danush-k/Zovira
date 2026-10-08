<script setup>
import { computed } from 'vue';
import { BookOpen, CookingPot, Dumbbell, Footprints, Headphones, LayoutGrid, Laptop, Shirt, Smartphone, Sofa, Sparkles, Watch, } from '@lucide/vue';
const props = defineProps({
    name: null
});
const icons = {
    smartphone: Smartphone,
    headphones: Headphones,
    laptop: Laptop,
    shirt: Shirt,
    footprints: Footprints,
    watch: Watch,
    sparkles: Sparkles,
    'cooking-pot': CookingPot,
    sofa: Sofa,
    dumbbell: Dumbbell,
    'book-open': BookOpen,
};
const icon = computed(() => (props.name && icons[props.name]) || LayoutGrid);
</script>

<template>
  <component :is="icon" aria-hidden="true" />
</template>
