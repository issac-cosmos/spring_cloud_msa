<template>
    <h1>store test</h1>
    <StoreTestComponentA/>
    <br><br>
    <StoreTestComponentB/>
</template>

<script>
import StoreTestComponentA from './StoreTestComponentA.vue';
import StoreTestComponentB from './StoreTestComponentB.vue';

export default{
    components:{
        StoreTestComponentA, StoreTestComponentB
    }
}
</script>