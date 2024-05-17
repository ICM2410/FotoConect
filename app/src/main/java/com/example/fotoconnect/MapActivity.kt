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
import android.location.Geocoder
import android.location.GpsStatus
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.fotoconnect.databinding.ActivityMapBinding
import com.example.taller3.ApiService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import kotlin.math.cos
class MapActivity : AppCompatActivity(), MapListener {
    private lateinit var mMap: MapView
    private lateinit var controller: IMapController
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var addressField: EditText
    private val markersMap = hashMapOf<String, Marker>()
    private var roadOverlay: Polyline? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null
    private lateinit var lightEventListener: SensorEventListener
    private lateinit var temperatureListener: SensorEventListener
    private lateinit var humidityEventListener: SensorEventListener
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MapActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = Color.TRANSPARENT
        initializeSensors()
        initializeUI(binding)
        initializeMapAndSensors()
        setupLocationUpdates()
    }
    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

        lightEventListener = createLightSensorListener()
        temperatureListener = createTemperatureSensorListener()
        humidityEventListener = createHumiditySensorListener()

        if (lightSensor == null) {
            Toast.makeText(this, "No light sensor found!", Toast.LENGTH_SHORT).show()
        }
        if (temperatureSensor == null) {
            Toast.makeText(this, "No ambient temperature sensor found!", Toast.LENGTH_SHORT).show()
        }
        if (humiditySensor == null) {
            Toast.makeText(this, "No relative humidity sensor found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createLightSensorListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    val lux = event.values[0]
                    if (lux < 5000) {
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
                        runOnUiThread {
                            mMap.overlayManager.tilesOverlay.setColorFilter(null)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    private fun createTemperatureSensorListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                    val temperature = event.values[0]
                    val temperatureTextView = findViewById<TextView>(R.id.Temperature)
                    temperatureTextView.text = "$temperature°C"
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    private fun createHumiditySensorListener(): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_RELATIVE_HUMIDITY) {
                    val humidity = event.values[0]
                    val humidityTextView = findViewById<TextView>(R.id.Humidity)
                    humidityTextView.text = "$humidity%"
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    private fun initializeUI(binding: ActivityMapBinding) {
        val navigationButton1 = findViewById<View>(R.id.ic_camara)
        navigationButton1.setOnClickListener {
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
        }
        val notificationButton = findViewById<View>(R.id.notificationl)
        notificationButton.setOnClickListener {
            val intent = Intent(this, TakepicActivity::class.java)
            startActivity(intent)
        }
        val peopleButton = findViewById<View>(R.id.people)
        peopleButton.setOnClickListener {
            val intent = Intent(this, MyUserActivity::class.java)
            startActivity(intent)
        }
        val navigationButton = findViewById<View>(R.id.ic_mensaje)
        navigationButton.setOnClickListener {
            val intent = Intent(this, MensajeActivity::class.java)
            startActivity(intent)
        }
        addressField = findViewById(R.id.address_field)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val addressString = addressField.text.toString()
            if (addressString.isNotEmpty()) {
                searchAddressOrMarker(addressString)
            }
        }
        val locateButton = findViewById<Button>(R.id.button3)
        locateButton.setOnClickListener {
            mMyLocationOverlay.myLocation?.let { location ->
                val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                mMap.controller.setCenter(userGeoPoint)
                mMap.controller.setZoom(18.5)
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

    private fun initializeMapAndSensors() {
        Configuration.getInstance().load(applicationContext, getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE))
        mMap = findViewById(R.id.osmmap)
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.setMultiTouchControls(true)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            initializeMap()
            fetchAndPlaceUserLocations()
        }
    }

    private fun initializeMap() {
        controller = mMap.controller
        controller.setZoom(17.5)
        val locationProvider = GpsMyLocationProvider(this)
        mMyLocationOverlay = MyLocationNewOverlay(locationProvider, mMap)
        mMyLocationOverlay.enableMyLocation()
        mMyLocationOverlay.enableFollowLocation()
        mMyLocationOverlay.isDrawAccuracyEnabled = true
        mMyLocationOverlay.runOnFirstFix {
            runOnUiThread {
                if (mMyLocationOverlay.myLocation != null) {
                    controller.animateTo(mMyLocationOverlay.myLocation)
                }
            }
        }
        mMap.overlays.add(mMyLocationOverlay)
        mMap.invalidate()
    }

    private fun fetchAndPlaceUserLocations() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateMarker(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateMarker(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                name?.let {
                    markersMap[it]?.let { marker ->
                        mMap.overlays.remove(marker)
                        markersMap.remove(it)
                        mMap.invalidate()
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapActivity, "Failed to load user locations: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Database error: ${error.message}")
            }
        })
    }

    private fun updateMarker(snapshot: DataSnapshot) {
        val latitude = snapshot.child("latitude").getValue(Double::class.java)
        val longitude = snapshot.child("longitude").getValue(Double::class.java)
        val name = snapshot.child("name").getValue(String::class.java)
        val profilePictureUrl = snapshot.child("contactImage").getValue(String::class.java)
        if (latitude != null && longitude != null && name != null && profilePictureUrl != null) {
            val geoPoint = GeoPoint(latitude, longitude)
            if (markersMap.containsKey(name)) {
                markersMap[name]?.position = geoPoint
                markersMap[name]?.let { mMap.overlays.add(it) }
            } else {
                addMarkerAtLocation(geoPoint, name, profilePictureUrl)
            }
            mMap.invalidate()

            // Check if my location is available and create a route
            mMyLocationOverlay.myLocation?.let { myLocation ->
                // Clear the previous route
                roadOverlay?.let { mMap.overlays.remove(it) }
                // Create a new route
                createRoute(GeoPoint(myLocation.latitude, myLocation.longitude), geoPoint)
            }
        }
    }

    private fun searchAddressAndDrawRoute(addressOrTitle: String) {
        markersMap[addressOrTitle]?.let { marker ->
            mMap.controller.setCenter(marker.position)
            mMap.controller.setZoom(18.5)
            mMyLocationOverlay.myLocation?.let { myLocation ->
                createRoute(GeoPoint(myLocation.latitude, myLocation.longitude), marker.position)
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
                        createRoute(GeoPoint(myLocation.latitude, myLocation.longitude), geoPoint)
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

    private fun createRoute(start: GeoPoint, end: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            val startCoords = "${start.longitude},${start.latitude}"
            val endCoords = "${end.longitude},${end.latitude}"
            val call = getRetrofit().create(ApiService::class.java)
                .getRoute("5b3ce3597851110001cf6248d2958fc5b5ed4482894bb94a12cdc98b", startCoords, endCoords)
            if (call.isSuccessful) {
                drawRoute(call.body())
            } else {
                Log.i(TAG, "Route API Call Failed")
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = Polyline()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.addPoint(GeoPoint(it[1], it[0]))
        }
        runOnUiThread {
            roadOverlay?.let { mMap.overlays.remove(it) }
            roadOverlay = polyLineOptions
            mMap.overlays.add(roadOverlay)
            mMap.invalidate()
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
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

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }
        mMap.overlays.add(marker)
        mMap.invalidate()
        if (title != null) {
            markersMap[title] = marker
        }
    }

    private fun setupAddressInput() {
        addressField = findViewById(R.id.address_field)
        addressField.imeOptions = EditorInfo.IME_ACTION_SEND
        addressField.setOnEditorActionListener { v, actionId, _ ->
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

    private fun searchAddressOrMarker(addressOrTitle: String) {
        markersMap[addressOrTitle]?.let {
            mMap.controller.setCenter(it.position)
            mMap.controller.setZoom(18.5)
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
                    mMap.controller.setZoom(18.5)
                }
            } else {
                Toast.makeText(this, "Address or marker not found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error while searching: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateUserLocation(location)
                }
            }
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun getLastKnownLocationAndUpdateUser() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    updateUserLocation(it)
                }
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    private fun updateUserLocation(location: Location) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
            userReference.child("latitude").setValue(location.latitude)
            userReference.child("longitude").setValue(location.longitude)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Location updated successfully")
                    } else {
                        Log.e(TAG, "Failed to update location")
                    }
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastKnownLocationAndUpdateUser()
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mMap.onResume()
        registerSensorListeners()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        mMap.onPause()
        unregisterSensorListeners()
        stopLocationUpdates()
    }

    private fun registerSensorListeners() {
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(temperatureListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(humidityEventListener, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun unregisterSensorListeners() {
        sensorManager.unregisterListener(lightEventListener)
        sensorManager.unregisterListener(temperatureListener)
        sensorManager.unregisterListener(humidityEventListener)
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return false
    }
}
