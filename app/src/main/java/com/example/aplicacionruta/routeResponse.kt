package com.example.aplicacionruta

import com.google.gson.annotations.SerializedName

data class routeResponse(@SerializedName("features") val features:List<Feature>)
data class Feature(@SerializedName("geometry") val geometry:Geometry)
data class Geometry(@SerializedName("coordinates") val coordinates:List<List<Double>>)