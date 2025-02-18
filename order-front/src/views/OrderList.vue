<template>
<OrderListComponent
:isMyPage="false"
/>
</template>
<script>
import OrderListComponent from '@/components/OrderListComponent.vue';

export default{
    components:{
        OrderListComponent
    },
    data(){
        return{

        }
    }
}
</script>