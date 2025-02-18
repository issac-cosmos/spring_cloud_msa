function initState(){
    return {
        count:0,
        message:"",
    }
}

const practice ={
    // 상태 초기화
    state: initState,
    // 값을 변경하는 메서드
    // mutation은 컴포넌트에서 직접 호출되기 보다는, actions를 통해 mutation을 호출
    // 그이유는 여러 mutation의 조합을 actions에서 정의할수 있기 때문.
    mutations:{
        incrementCount(state){
            state.count++;
        },
        updateMessage(state, newMessage){
            state.message = newMessage;
        }
    },
    actions:{
        increment(context){
            // vuex에서 mutations를 호출할때는 commit메서드 사용
            context.commit('incrementCount');
        },
        updateMessage(context,newMessage){
            context.commit('updateMessage', newMessage);
        }
    },
    // 값을 가져가기위한 메서드
    getters:{
        getCount: state => state.count,
        getMessage: state => state.message,
    },
}
export default practice;