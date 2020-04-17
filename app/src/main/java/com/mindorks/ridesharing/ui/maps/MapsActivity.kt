package com.mindorks.ridesharing.ui.maps

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.*
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, MapsView {

    private lateinit var mMap: GoogleMap
    private lateinit var presenter: MapsPresenter
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var currentLatLng: LatLng? = null
    private var pickUpLatLng: LatLng? = null
    private var dropLatLng: LatLng? = null
    private var greyPolyLine: Polyline? = null
    private var blackPolyLine: Polyline? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private val nearByCabsMarkerList = arrayListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        presenter = MapsPresenter((NetworkService()))
        presenter.onAttach(this)
        setupClickListener()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun setupClickListener() {
        pickUpTextView.setOnClickListener {
            launchLocationAutocompleteActivity(Constants.PICKUP_REQUEST_CODE)
        }
        dropTextView.setOnClickListener {
            launchLocationAutocompleteActivity(Constants.DROP_REQUEST_CODE)
        }
        requestCabButton.setOnClickListener {
            statusTextView.visibility = View.VISIBLE
            statusTextView.text = getString(R.string.requesting_your_cab)
            requestCabButton.isEnabled = false
            pickUpTextView.isEnabled = false
            dropTextView.isEnabled = false
            presenter.requestCab(pickUpLatLng!!, dropLatLng!!)
        }
    }

    override fun onStart() {
        super.onStart()
        when {
            PermissionUtils.isAccessFineLocationGranted(this) -> {
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        setupLocationListener()
                    }
                    else -> {
                        PermissionUtils.showGPSNotEnableDialog(this)
                    }
                }
            }
            else -> {
                PermissionUtils.requestAccessFindLocationPermission(
                    this,
                    Constants.LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }


    private fun setCurrentLocationAsPickup() {
        pickUpLatLng = currentLatLng
        pickUpTextView.text = getString(R.string.current_location)
    }

    private fun launchLocationAutocompleteActivity(requestCode: Int) {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val intent =
            Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
        startActivityForResult(intent, requestCode)
    }


    private fun checkAndShowRequestButton() {
        if (pickUpLatLng != null && dropLatLng != null) {
            requestCabButton.visibility = View.VISIBLE
            requestCabButton.isEnabled = true
        }
    }

    private fun moveCamera(latLng: LatLng?) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng?) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom((15.5f)).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun enableMyLocationOnMap() {
        mMap.setPadding(0, ViewUtils.dpToPx(48f), 0, 0)
        mMap.isMyLocationEnabled = true
    }

    private fun setupLocationListener() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        //for current location
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (currentLatLng == null) {
                    for (location in locationResult.locations) {
                        if (currentLatLng == null) {
                            currentLatLng = LatLng(location.latitude, location.longitude)
                            setCurrentLocationAsPickup()
                            enableMyLocationOnMap()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                            presenter.requestNearByCabs(currentLatLng!!)
                        }
                    }
                }
            }
        }

        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    private fun addCarMarkerandGet(latLng: LatLng?): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(this))
        return mMap.addMarker(MarkerOptions().position(latLng!!).flat(true).icon(bitmapDescriptor))
    }

    private fun addOriginDestinationMarkerandGet(latLng: LatLng?): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getDestinationBitmap())
        return mMap.addMarker(MarkerOptions().position(latLng!!).flat(true).icon(bitmapDescriptor))
    }

    override fun showNearByCabs(latlngList: List<LatLng>) {
        nearByCabsMarkerList.clear()
        for (latlng in latlngList) {
            val nearbyCabMarker = addCarMarkerandGet(latlng)
            nearByCabsMarkerList.add(nearbyCabMarker)
        }
    }

    override fun informCabBooked() {
        nearByCabsMarkerList.forEach {
            it.remove()
        }
        nearByCabsMarkerList.clear()
        requestCabButton.visibility = View.GONE
        statusTextView.text = getString(R.string.your_cab_is_booked)
    }


    override fun showPath(latlngList: List<LatLng>) {
        Log.e("TAG", "showPath")
        val builder = LatLngBounds.builder()
        for (latLng in latlngList) {
            builder.include(latLng)
        }
        val bound = builder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bound, 2))
        val polyLineOptions = PolylineOptions()
        polyLineOptions.run {
            color(Color.GRAY)
            width(5f)
            addAll(latlngList)
        }
        greyPolyLine = mMap.addPolyline(polyLineOptions)
        val blackPolyLineOptions = PolylineOptions()
        polyLineOptions.run {
            color(Color.GRAY)
            width(5f)
        }
        blackPolyLine = mMap.addPolyline(blackPolyLineOptions)

        originMarker = addOriginDestinationMarkerandGet(latlngList[0])
        originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker = addOriginDestinationMarkerandGet(latlngList[latlngList.size-1])
        destinationMarker?.setAnchor(0.5f, 0.5f)

        val polyLineAnimator = AnimationUtils.polyLineAnimator()
        polyLineAnimator.addUpdateListener {
            val percentValue = (it.animatedValue as Int)
            val index = (greyPolyLine?.points?.size)!! * (percentValue/100.0f).toInt()
            blackPolyLine?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polyLineAnimator.start()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            setupLocationListener()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnableDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(this, "Location Permission not granted", Toast.LENGTH_LONG)
                        .show()
                }

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.PICKUP_REQUEST_CODE || requestCode == Constants.DROP_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    when (requestCode) {
                        Constants.PICKUP_REQUEST_CODE -> {
                            pickUpTextView.text = place.name
                            pickUpLatLng = place.latLng
                            checkAndShowRequestButton()
                        }
                        Constants.DROP_REQUEST_CODE -> {
                            dropTextView.text = place.name
                            dropLatLng = place.latLng
                            checkAndShowRequestButton()
                        }
                    }
                }

                Activity.RESULT_CANCELED -> {

                }
            }
        }
    }


    override fun onDestroy() {
        presenter.onDetach()
        super.onDestroy()
    }


}
