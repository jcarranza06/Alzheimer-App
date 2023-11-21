package com.example.aplicacionruta.classes

import android.location.Location

class SerializablePolygon {
    lateinit var coords:ArrayList<Array<Double>>

    fun add(lat:Double, lon:Double){
        coords.add(arrayOf<Double>(lat, lon))
    }
    // initializer block
    init {
        coords = ArrayList<Array<Double>>()
    }
}