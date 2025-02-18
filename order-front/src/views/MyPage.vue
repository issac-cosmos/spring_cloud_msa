<template>
    <v-container>
        <v-row justify="center">
            <v-col>
                <v-card>
                    <v-card-title class="text-center text-h5">
                        회원정보    
                    </v-card-title>
                    <v-card-text>
                        <v-table>
                            <t-body>
                                <tr>
                                    <td>id : </td><td>{{ memberInfo.id }}</td>
                                </tr>
                                <tr>
                                    <td>이름 : </td><td>{{ memberInfo.name }}</td>
                                </tr>
                                <tr>
                                    <td>이메일 : </td><td>{{ memberInfo.email }}</td>
                                </tr>
                                <tr>
                                    <td>주문건수 : </td><td>{{ memberInfo.orderCount }}</td>
                                </tr>
                            </t-body>
                        </v-table>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>
    </v-container>
    <OrderListComponent
    :isMyPage="true"
    />
</template>

<script>
import OrderListComponent from '@/components/OrderListComponent.vue';
import axios from 'axios';

export default{
    data(){
        return{
            memberInfo:"",
        }
    },
    async created(){
        const response = await axios.get(`${process.env.VUE_APP_API_BASE_URL}/member/myinfo`);
        this.memberInfo = response.data;
    },
    components:{
        OrderListComponent
    }
}
</script>