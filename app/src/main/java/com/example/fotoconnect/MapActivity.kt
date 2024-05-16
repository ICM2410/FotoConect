package com.example.fotoconnect

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.location.Geocoder
import android.location.GpsStatus
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.fotoconnect.databinding.ActivityMapBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Date
import java.util.Locale
import kotlin.math.cos


class MapActivity : AppCompatActivity(), MapListener, GpsStatus.Listener {
    lateinit var mMap: MapView
    lateinit var controller: IMapController
    lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private var lastRecordedLocation: Location? = null
    private var userMarker: Marker? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var addressField: EditText
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener
    private val markersMap = hashMapOf<String, Marker>()
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private lateinit var humidityEventListener: SensorEventListener
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val policy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        } else {
            TODO("VERSION.SDK_INT < GINGERBREAD")
        }
        val navigationButton1 = findViewById<View>(R.id.ic_camara)
        navigationButton1.setOnClickListener {
        // Start FeedActivity here
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }
        val notificationButton = findViewById<View>(R.id.notificationl)
        notificationButton.setOnClickListener {
            // Start NotificationActivity here
            val intent = Intent(this, TakepicActivity::class.java)
            startActivity(intent)
        }
        val peopleButton = findViewById<View>(R.id.people)
        peopleButton.setOnClickListener {
            // Start NotificationActivity here
            val intent = Intent(this, MyUserActivity::class.java)
            startActivity(intent)
        }
        val navigationButton = findViewById<View>(R.id.ic_mensaje)
        navigationButton.setOnClickListener {
            // Start FeedActivity here
            val intent = Intent(this, MensajeActivity::class.java)
            startActivity(intent)
        }
        StrictMode.setThreadPolicy(policy)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor == null) {
            Toast.makeText(this, "No light sensor found!", Toast.LENGTH_SHORT).show()
        }
        lightEventListener = createLightSensorListener()
        val roadManager = OSRMRoadManager(this,"Android")
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        mMap = binding.osmmap
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.mapCenter
        mMap.setMultiTouchControls(true)
        mMap.getLocalVisibleRect(Rect())
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val addressString = addressField.text.toString()
            if (addressString.isNotEmpty()) {
                searchAddressOrMarker(addressString)
            }
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)
        if (humiditySensor == null) {
            Toast.makeText(this, "No relative humidity sensor found!", Toast.LENGTH_SHORT).show()
        } else {
            humidityEventListener = createHumiditySensorListener()
            sensorManager.registerListener(humidityEventListener, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        if (temperatureSensor == null) {
            Toast.makeText(this, "No ambient temperature sensor found!", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager.registerListener(temperatureListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            initializeMap()
            initializeMarkers()
            fetchAndPlaceUserLocations()  // Add this line to fetch and place user locations

        }
        addressField = findViewById(R.id.address_field)
        val locateButton = findViewById<Button>(R.id.button3)
        locateButton.setOnClickListener {
            mMyLocationOverlay.myLocation?.let { location ->
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                mMap.controller.setCenter(userGeoPoint)
                mMap.controller.setZoom(18.5)  // You can adjust the zoom level as needed
            } ?: Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
        }
        val routeButton = findViewById<Button>(R.id.button2)
        routeButton.setOnClickListener {
            val addressString = addressField.text.toString()
            if (addressString.isNotEmpty()) {
                searchAddressAndDrawRoute(addressString)
            }
        }
        setupAddressInput()
    }
    private fun fetchAndPlaceUserLocations() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val latitude = userSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = userSnapshot.child("longitude").getValue(Double::class.java)
                    val name = userSnapshot.child("name").getValue(String::class.java)
                    val profilePictureUrl = userSnapshot.child("contactImage").getValue(String::class.java)
                    if (latitude != null && longitude != null && name != null && profilePictureUrl != null) {
                        val geoPoint = GeoPoint(latitude, longitude)
                        addMarkerAtLocation(geoPoint, name, profilePictureUrl)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapActivity, "Failed to load user locations: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("MapActivity", "Database error: ${error.message}")
            }
        })
    }

    private val temperatureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                val temperature = event.values[0]  // Temperature in degrees Celsius
                val temperatureTextView = findViewById<TextView>(R.id.Temperature)
                temperatureTextView.text = "$temperature°C"
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Handle sensor accuracy changes if necessary
        }
    }
    private fun initializeMyLocationOverlay(profilePictureUrl: String?) {
        val gpsMyLocationProvider = GpsMyLocationProvider(this)
        mMyLocationOverlay = MyLocationNewOverlay(gpsMyLocationProvider, mMap).apply {
            enableMyLocation()
            enableFollowLocation()
            isDrawAccuracyEnabled = true
        }

        if (profilePictureUrl != null) {
            Glide.with(this)
                .asBitmap()
                .load(profilePictureUrl)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val resizedBitmap = Bitmap.createScaledBitmap(resource, 100, 100, false)
                        mMyLocationOverlay.setPersonIcon(BitmapDrawable(resources, resizedBitmap).bitmap)
                        mMap.invalidate()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle cleanup if needed
                    }
                })
        }

        mMap.overlays.add(mMyLocationOverlay)
    }

    private fun createHumiditySensorListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_RELATIVE_HUMIDITY) {
                    val humidity = event.values[0]  // Relative humidity in percent
                    val humidityTextView = findViewById<TextView>(R.id.Humidity)
                    humidityTextView.text = "$humidity%"
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                
            }
        }
    }
    private fun searchAddressAndDrawRoute(addressOrTitle: String) {
        
        markersMap[addressOrTitle]?.let { marker ->
            mMap.controller.setCenter(marker.position)
            mMap.controller.setZoom(18.5)
            mMyLocationOverlay.myLocation?.let { myLocation ->
                drawRoute(GeoPoint(myLocation.latitude, myLocation.longitude), marker.position)
            }
            return
        }
        val geocoder = Geocoder(this)
        try {
            val addresses = geocoder.getFromLocationName(addressOrTitle, 1)
            if (addresses!!.isNotEmpty()) {
                val location = addresses[0]
                if (location.hasLatitude() && location.hasLongitude()) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    mMap.controller.setCenter(geoPoint)
                    mMap.controller.setZoom(14.5)
                    addMarkerAtLocation(geoPoint, addressOrTitle)
                    mMyLocationOverlay.myLocation?.let { myLocation ->
                        drawRoute(GeoPoint(myLocation.latitude, myLocation.longitude), geoPoint)
                    }
                }
            } else {
                Toast.makeText(this, "Address or marker not found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error while searching: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun createLightSensorListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    
                    val lux = event.values[0]
                    if (lux < 5000) {
                        // Assume dark style
                        runOnUiThread {
                            
                            mMap.overlayManager.tilesOverlay.setColorFilter(
                                ColorMatrixColorFilter(floatArrayOf(
                                    -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                                    0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                                    0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                                ))
                            )
                        }
                    } else {
                        // Assume light style
                        runOnUiThread {
                            mMap.overlayManager.tilesOverlay.setColorFilter(null)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                
            }
        }
    }
    override fun onResume() {
        super.onResume()
        mMap.onResume()
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(humidityEventListener, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL)
        mMap.onPause()
        sensorManager.unregisterListener(lightEventListener)
        super.onResume()
        mMap.onResume() // Resume the map view
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.enableMyLocation()
            mMyLocationOverlay.enableFollowLocation()
        }
        temperatureSensor?.let {
            sensorManager.registerListener(temperatureListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.unregisterListener(humidityEventListener)
    }
    private fun setupAddressInput() {
        addressField = findViewById(R.id.address_field)
        addressField.imeOptions = EditorInfo.IME_ACTION_SEND
        addressField.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val inputString = v.text.toString()
                if (inputString.isNotEmpty()) {
                    searchAddressOrMarker(inputString)
                }
                true
            } else {
                false
            }
        }

    }
    private fun initializeMarkers() {
    }
    class MyLocation(val date: Date, val latitude: Double, val longitude: Double) {
        @RequiresApi(Build.VERSION_CODES.N)
        fun toJSON(): JSONObject {
            val obj = JSONObject()
            obj.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date))
            obj.put("latitude", latitude)
            obj.put("longitude", longitude)
            return obj
        }
    }
    private fun searchAddressOrMarker(addressOrTitle: String) {
        // Check if it's a known marker title and center the map on it
        markersMap[addressOrTitle]?.let {
            mMap.controller.setCenter(it.position)
            mMap.controller.setZoom(18.5)
            return
        }

        // If no marker found, proceed to geocode the address
        val geocoder = Geocoder(this)
        try {
            val addresses = geocoder.getFromLocationName(addressOrTitle, 1)
            if (addresses!!.isNotEmpty()) {
                val location = addresses[0]
                if (location.hasLatitude() && location.hasLongitude()) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    mMap.controller.setCenter(geoPoint)
                    mMap.controller.setZoom(18.5)
                    // Do not add a marker at the searched location
                }
            } else {
                Toast.makeText(this, "Address or marker not found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error while searching: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private val RADIUS_OF_EARTH_KM = 6371.0
    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        return Math.round(result * 100.0) / 100.0
    }
    private var roadOverlay: Polyline? = null
    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val roadManager = OSRMRoadManager(this,"Android")
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        // Calculate distance and display it
        val dist = distance(start.latitude, start.longitude, finish.latitude, finish.longitude)
        Toast.makeText(this, "Distancia: $dist km", Toast.LENGTH_LONG).show()
        roadOverlay?.let {
            mMap.overlays.remove(it)
        }
        roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay?.outlinePaint?.color = Color.RED
        roadOverlay?.outlinePaint?.strokeWidth = 10f
        roadOverlay?.let {
            mMap.overlays.add(it)
        }
        mMap.invalidate()
    }
    private fun initializeMap() {
        // Configure the map controller and overlays
        controller = mMap.controller
        controller.setZoom(17.5)
        // Setup the location overlay
        val locationProvider = GpsMyLocationProvider(this)
        mMyLocationOverlay = MyLocationNewOverlay(locationProvider, mMap)
        mMyLocationOverlay.enableMyLocation() // Enable location updates
        mMyLocationOverlay.enableFollowLocation() // Map follows the location
        mMyLocationOverlay.isDrawAccuracyEnabled = true
        // Handle initial location fix to center the map on the current location
        mMyLocationOverlay.runOnFirstFix {
            runOnUiThread {
                if (mMyLocationOverlay.myLocation != null) {
                    controller.animateTo(mMyLocationOverlay.myLocation)
                }
            }
        }
        // Add the overlay to the map
        mMap.overlays.add(mMyLocationOverlay)
        mMap.invalidate() // Refresh the map
    }
    override fun onPause() {
        super.onPause()
        mMap.onPause() // Pause the map view
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.disableMyLocation()
            mMyLocationOverlay.disableFollowLocation()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
            fetchAndPlaceUserLocations()  // Add this line to fetch and place user locations
        }
    }
    override fun onScroll(event: ScrollEvent?): Boolean {
        return true
    }
    override fun onZoom(event: ZoomEvent?): Boolean {
        return false
    }
    override fun onGpsStatusChanged(event: Int) {
    }
    private fun addMarkerAtLocation(location: GeoPoint, title: String? = null, imageUrl: String? = null) {
        if (title != null && markersMap.containsKey(title)) {
            mMap.overlays.remove(markersMap[title])
        }
        val marker = Marker(mMap).apply {
            position = location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title ?: "Unnamed location"
        }
        if (imageUrl != null) {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .circleCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val resizedBitmap = Bitmap.createScaledBitmap(resource, 100, 100, false)
                        marker.icon = BitmapDrawable(resources, resizedBitmap)
                        mMap.invalidate()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle cleanup if needed
                    }
                })
        }
        mMap.overlays.add(marker)
        mMap.invalidate()
        if (title != null) {
            markersMap[title] = marker
        }
    }
    fun writeJSONObject(location: Location) {
        val myLocation = MyLocation(Date(), location.latitude, location.longitude)
        val filename = "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)
        val locations = JSONArray()
        if (file.exists()) {
            val content = file.readText()
            if (content.isNotEmpty()) {
                val currentData = JSONArray(content)
                for (i in 0 until currentData.length()) {
                    locations.put(currentData.getJSONObject(i))
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locations.put(myLocation.toJSON())
        }
        file.writeText(locations.toString())
        Log.i("LOCATION", "File modified at path: $file")
    }
    private fun recordLocation(newLocation: Location) {
        val distanceThreshold = 30
        if (lastRecordedLocation != null && newLocation.distanceTo(lastRecordedLocation!!) > distanceThreshold) {
            writeJSONObject(newLocation)
            lastRecordedLocation = newLocation
        } else if (lastRecordedLocation == null) {
            writeJSONObject(newLocation)
            lastRecordedLocation = newLocation
        }
    }
    private fun writeLocationToFile(location: Location) {
        val dateFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        } else {
            TODO("VERSION.SDK_INT < N")
        }
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dateFormat.format(Date())
        } else {
            TODO("VERSION.SDK_INT < N")
        }

        val jsonObject = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("datetime", currentDate)
        }

        val jsonArray = JSONArray()
        jsonArray.put(jsonObject)

        val file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "location_records.json")
        } else {
            TODO("VERSION.SDK_INT < KITKAT")
        }
        try {
            val fileWriter = FileWriter(file, true)
            fileWriter.write(jsonArray.toString())
            fileWriter.close()
            Log.d("MapActivity", "Location recorded: $jsonObject")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
