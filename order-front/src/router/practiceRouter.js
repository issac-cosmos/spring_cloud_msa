import VuetifyComponent from "@/components/practice/VuetifyComponent.vue"
import ModelingComponent from "@/components/practice/ModelingComponent.vue"
import HookComponent from "@/components/practice/HookComponent.vue"
import ConditionalAxiosComponent from "@/components/practice/ConditionalAxiosComponent.vue"
import StoreTestComponent from "@/components/practice/StoreTestComponent.vue"

export const practiceRouter = [
    {
        path: '/practice/vuetify',
        name: 'VuetifyComponent',//name은 선택사항임.
        component: VuetifyComponent
    },
    {
        path: '/practice/modeling',
        name: 'ModelingComponent',
        component: ModelingComponent
    },
    {
        path: '/practice/hooks',
        name: 'HookComponent',
        component: HookComponent
    },
    {
        path: '/practice/contional',
        name: 'ConditionalAxiosComponent',
        component: ConditionalAxiosComponent
    },
    {
        path: '/practice/storetest',
        name: 'StoreTestComponent',
        component: StoreTestComponent
    },
]
