package com.github.yhanada.mockmylocation.data

import com.github.yhanada.mockmylocation.model.MyLocation

class DataSource {
    companion object {
        fun getLocations(): List<MyLocation> = listOf(
            MyLocation("Apple心斎橋", 34.6717868,135.4989041),
            MyLocation("難波神社", 34.6785897,135.4995511),
            MyLocation("アメリカ村", 34.6723123,135.4978135),
        )
    }
}
