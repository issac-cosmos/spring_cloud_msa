<template>
    <h1>hello world {{ language }}</h1>
    <!-- v-model문법을 사용하면 양방향 데이터 바인딩 -->
    <input type="text" v-model="value">
    <button @click="showValue()">변수변경사항확인</button>
    <button @click="changeValue()">변수체인지</button>
</template>
<script>
export default{
    data(){
        return{
            language:"java",
            value: "python"
        }
    },
    // 화면이 생성되기전에 실행되는 hook함수
    created(){
        alert("created 함수호출")
    },
    methods: {
        showValue(){
            alert(this.value);
        },
        changeValue(){
            this.value = "C++"
        }
    },
}
</script>