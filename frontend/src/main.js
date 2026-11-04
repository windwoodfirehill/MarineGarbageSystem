import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import 'leaflet/dist/leaflet.css'
import App from './App.vue'
createApp(App).use(createVuetify({components,directives,defaults:{VBtn:{color:'primary'}},theme:{defaultTheme:'marine',themes:{marine:{dark:false,colors:{primary:'#1a73e8',secondary:'#0b8060',background:'#f6f8fc',surface:'#ffffff'}}}}})).mount('#app')
