<template>
    <h1>store component A test</h1>
    <p>일반 변수값 count : {{ count }}</p>
    <v-btn @click="incrementCount()"> count값 증가 </v-btn>

    <p>store 변수값 count : {{ getStoreCount }}</p>
    <v-btn @click="incrementStoreCount()"> count값 증가 </v-btn>
<br><br>
    <input type="text" v-model="message">
    <p>store 변수값 message : {{ getStoreMessage }}</p>
    <v-btn @click="updateMessage()"> store message 변경 </v-btn>

</template>

<script>
export default{
    data(){
        return{
            count: 0,
            message:""
        }
    },
    computed:{
        getStoreCount(){
            return this.$store.getters.getCount;
        },
        getStoreMessage(){
            return this.$store.getters.getMessage;
        }
    },
    methods: {
        incrementCount(){
            this.count++;
        },
        incrementStoreCount(){
            // vuex에서 actions를 호출할때는 dispatch를 사용
            this.$store.dispatch('increment');
        },
        updateMessage(){
            this.$store.dispatch('updateMessage',this.message);
        }
    }
}
</script>