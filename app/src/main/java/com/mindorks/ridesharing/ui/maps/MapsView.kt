package com.mindorks.ridesharing.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView  {

    fun showNearByCabs(latlngList: List<LatLng>)

    fun informCabBooked()

    fun showPath(latlngList: List<LatLng>)
}