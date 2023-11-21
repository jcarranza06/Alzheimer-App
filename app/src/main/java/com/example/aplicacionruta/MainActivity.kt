package com.example.aplicacionruta

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aplicacionruta.classes.SerializablePolygon
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.libraries.places.api.Places
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapa: GoogleMap
    private lateinit var btnRuta: Button
    private lateinit var btnPoly: Button
    private lateinit var btnEndPoly: Button
    private lateinit var tempPolyline: Polyline
    private lateinit var optionsTempPolyline: PolylineOptions
    private lateinit var allowedGeoPolygons: ArrayList<SerializablePolygon>

    private var start:String = "" //-3.054310865700245,40.8067371071451
    private var end:String = "" //-2.1589237824082375,39.53949681233611

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)


    //Polygon options
    private val COLOR_WHITE_ARGB = -0x1
    private val COLOR_BLACK_ARGB = -0x0
    private val COLOR_DARK_GREEN_ARGB = -0xc771c4
    private val COLOR_LIGHT_GREEN_ARGB = -0x7e387c
    private val COLOR_DARK_ORANGE_ARGB = -0xa80e9
    private val COLOR_LIGHT_ORANGE_ARGB = -0x657db
    private val POLYGON_STROKE_WIDTH_PX = 8
    private val PATTERN_DASH_LENGTH_PX = 20

    private val DASH: PatternItem = Dash(PATTERN_DASH_LENGTH_PX.toFloat())


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRuta = findViewById(R.id.btnRuta)
        btnPoly = findViewById(R.id.btnDefPolyLine)
        btnEndPoly = findViewById(R.id.btnEndPolyLine)

        //
        allowedGeoPolygons = ArrayList<SerializablePolygon>()
        // Construct a PlacesClient
        Places.initialize(applicationContext, "AIzaSyDcv9Xpzfg1Ae6wVvTCSROrtzdZ9QgWXGU")
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getSerializedPolygons()

        btnRuta.setOnClickListener {
            if(::mapa.isInitialized){
                mapa.setOnMapClickListener {
                    println(it.longitude)
                    println(it.latitude)
                    if (start.isEmpty()) {
                        start = "${it.longitude},${it.latitude}"
                    } else if (end.isEmpty()) {
                        end = "${it.longitude},${it.latitude}"
                        CreateRoute()
                    }
                }
            }
        }
        val pointsPosibleMerkedArea = LinkedHashMap<Marker, Array<Double>>()
        optionsTempPolyline = PolylineOptions().clickable(true)
        btnPoly.setOnClickListener {
            if(::mapa.isInitialized){
                btnEndPoly.visibility = View.VISIBLE
                btnPoly.visibility = View.GONE

                mapa.setOnMapClickListener {
                    println(it.longitude)
                    println(it.latitude)
                    if (pointsPosibleMerkedArea.size > 0){
                        tempPolyline?.remove()
                    }
                    val perth = mapa.addMarker(
                        MarkerOptions()
                            .title("Punto")
                            .snippet("Coords: "+it.latitude+"; "+it.longitude)
                            .position(LatLng(it.latitude, it.longitude))
                            .flat(true)
                    )
                    if (perth != null) {
                        pointsPosibleMerkedArea[perth] = arrayOf(it.latitude, it.longitude)
                        optionsTempPolyline.add(LatLng(it.latitude,it.longitude))
                        tempPolyline = mapa.addPolyline(optionsTempPolyline)
                    }
                }
            }
        }

        btnEndPoly.setOnClickListener{
            btnEndPoly.visibility = View.GONE
            btnPoly.visibility = View.VISIBLE
            val options = PolygonOptions()
                .clickable(true)
            for ((clave, valor) in pointsPosibleMerkedArea) {
                println("Clave: $clave, Valor: $valor")
                clave?.remove()
                options.add(LatLng(valor[0], valor[1]))
            }
            pointsPosibleMerkedArea.clear()
            tempPolyline?.remove()
            optionsTempPolyline = PolylineOptions().clickable(true)

            val polyline1 = mapa.addPolygon(options)
            stylePolygon(polyline1)
            //allowedGeoPolygons.add(polyline1)

            val puntosPolygon = polyline1?.points
            val newSerPolygon:SerializablePolygon = SerializablePolygon()
            // Imprimir los puntos del Polygon
            puntosPolygon?.forEachIndexed { index, latLng ->
                println("Punto $index: Latitud=${latLng.latitude}, Longitud=${latLng.longitude}")
                newSerPolygon.add(latLng.latitude,latLng.longitude)
            }
            allowedGeoPolygons.add(newSerPolygon)

            val gson = Gson()
            println(allowedGeoPolygons)
            val jsonString = gson.toJson(allowedGeoPolygons)
            println(jsonString)

            val preferencias = getSharedPreferences("puntosPermitidos", Context.MODE_PRIVATE)
            val editor = preferencias.edit()
            editor.putString("puntos", jsonString)
            editor.apply()

        }

        val mapFrag = supportFragmentManager.findFragmentById(R.id.mapa) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }

    fun getSerializedPolygons() {
        val gson = Gson()
        val preferencias = getSharedPreferences("puntosPermitidos", Context.MODE_PRIVATE)
        val valorLeido = preferencias.getString("puntos", "")

        val type = object : TypeToken<ArrayList<SerializablePolygon>>() {}.type
        allowedGeoPolygons = gson.fromJson(valorLeido, type)
        println(valorLeido)
        println(allowedGeoPolygons)
    }

    override fun onMapReady(mapa: GoogleMap) {
        this.mapa = mapa

        // Prompt the user for permission.
        getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

        //takes the allowed serialized polygons array and puts them in to the map
        startSerializedPolygons()
    }

    private fun startSerializedPolygons(){
        allowedGeoPolygons.forEach { serPolygon ->
            // Hacer algo con cada elemento
            val options = PolygonOptions()
                .clickable(true)
            serPolygon.coords.forEach { coordenada ->
                options.add(LatLng(coordenada[0], coordenada[1]))
            }
            stylePolygon(mapa.addPolygon(options))
        }
    }

    private fun stylePolygon(polygon: Polygon) {
        // Get the data object stored with the polygon.
        val type = polygon.tag?.toString() ?: ""
        var pattern: List<PatternItem>? = null
        var strokeColor = COLOR_BLACK_ARGB
        var fillColor = COLOR_WHITE_ARGB
        when (type) {
            "alpha" -> {
                // Apply a stroke pattern to render a dashed line, and define colors.
                //pattern = PATTERN_POLYGON_ALPHA
                strokeColor = COLOR_DARK_GREEN_ARGB
                fillColor = COLOR_LIGHT_GREEN_ARGB
            }
            "beta" -> {
                // Apply a stroke pattern to render a line of dots and dashes, and define colors.
                //pattern = PATTERN_POLYGON_BETA
                strokeColor = COLOR_DARK_ORANGE_ARGB
                fillColor = COLOR_LIGHT_ORANGE_ARGB
            }
        }
        polygon.strokePattern = pattern
        polygon.strokeWidth = POLYGON_STROKE_WIDTH_PX.toFloat()
        polygon.strokeColor = strokeColor
        polygon.fillColor = Color.argb(50, 20, 255, 0)
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mapa?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        mapa?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        mapa?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (mapa == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mapa?.isMyLocationEnabled = true
                mapa?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mapa?.isMyLocationEnabled = false
                mapa?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 5
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    fun CreateRoute(){
        CoroutineScope(Dispatchers.IO).launch {
            println("pidiendo")
            val call = getRetrofit().create(ApiService::class.java).getRoute("5b3ce3597851110001cf624821254cc0741048568609830e35dc1e65",start,end)
            println("pedido")
            if(call.isSuccessful){
                println("success")
                println(call.body())
                drawRoute(call.body())
            }else{
                println("not success")
            }
        }
    }

    private fun drawRoute( route:routeResponse?){
        val polyLineOptions = PolylineOptions()
        println(1)
        route?.features?.first()?.geometry?.coordinates?.forEach{
            println(2)
            println(it)
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
            val poly = mapa.addPolyline(polyLineOptions)
        }
    }

    fun getRetrofit():Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}