package com.mindorks.ridesharing.ui.maps

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.simulator.WebSocket
import com.mindorks.ridesharing.simulator.WebSocketListener
import com.mindorks.ridesharing.utils.Constants
import org.json.JSONObject
import kotlin.math.ln

class MapsPresenter(private  val networkService: NetworkService): WebSocketListener {

    private var view: MapsView? = null
    private lateinit var  webSocket: WebSocket


    fun onAttach(view: MapsView){
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun requestNearByCabs(latLng: LatLng){
        val jsonObject= JSONObject()
        jsonObject.put(Constants.TYPE, Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT, latLng.latitude)
        jsonObject.put(Constants.LNG, latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun onDetach(){
        webSocket.disconnect()
        view = null
    }

     override fun onConnect() {
         Log.e("TAG", "connect")
    }

    override fun onMessage(data: String) {
        Log.e("TAG", "msg")
        val jsonObject = JSONObject(data)
        when(jsonObject.getString(Constants.TYPE)){
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearByCabs(jsonObject)
            }
            Constants.CAB_BOOKED -> {
                view?.informCabBooked()
            }
            Constants.PICKUP_PATH , Constants.TRIP_PATH-> {
                val jsonArray = jsonObject.getJSONArray("path")
                val pickUpPath  = arrayListOf<LatLng>()
                for(i in 0 until jsonArray.length()){
                    val lat = ((jsonArray.get(i)) as JSONObject).getDouble(Constants.LAT)
                    val lng = ((jsonArray.get(i)) as JSONObject).getDouble(Constants.LNG)
                    val latlng = LatLng(lat, lng)
                    pickUpPath.add(latlng)
                }
                view?.showPath(pickUpPath)
            }

            Constants.LOCATION -> {
                val latCurrent = jsonObject.getDouble("lat")
                val lngCurrent = jsonObject.getDouble("lng")
                view?.updateCabLocation(LatLng(latCurrent, lngCurrent))
            }

            Constants.CAB_IS_ARRIVING -> {
                view?.informCabIsArriving()
            }

            Constants.CAB_ARRIVED -> {
                view?.informCabArrived()
            }

            Constants.TRIP_START -> {
                view?.informTripStart()
            }

            Constants.TRIP_END -> {
                view?.informTripEnd()
            }
        }
    }

    fun requestCab(pickupLatLng: LatLng, dropLatLng: LatLng){
        val jsonObject = JSONObject()
        jsonObject.put(Constants.TYPE, Constants.REQUEST_CABS)
        jsonObject.put("pickUpLat", pickupLatLng.latitude)
        jsonObject.put("pickUpLng", pickupLatLng.longitude)
        jsonObject.put("dropLat", dropLatLng.latitude)
        jsonObject.put("dropLng", dropLatLng.longitude)
        webSocket.sendMessage((jsonObject.toString()))
    }

    private fun handleOnMessageNearByCabs(jsonObject: JSONObject) {
        val nearbyCabLocations = ArrayList<LatLng>()
        val jsonArray = jsonObject.getJSONArray(Constants.LOCATIONS)
        for(i in 0 until jsonArray.length()){
            val lat = ((jsonArray.get(i)) as JSONObject).getDouble(Constants.LAT)
            val lng = ((jsonArray.get(i)) as JSONObject).getDouble(Constants.LNG)
            val latlng = LatLng(lat, lng)
            nearbyCabLocations.add(latlng)
        }
        view?.showNearByCabs(nearbyCabLocations)
    }

    override fun onDisconnect() {
        Log.e("TAG", "disconnect")
    }

    override fun onError(error: String) {
        Log.e("TAG", "$error")
        val jsonObject = JSONObject(error)
        when(jsonObject.getString(Constants.TYPE)){

            Constants.ROUTES_NOT_AVAILABLE -> {
                view?.showRoutesNotAvailableError()
            }

            Constants.DIRECTION_API_FAILED -> {
                view?.showDirectionApiFailedError("Direction API Failed: "+ jsonObject.getString(Constants.ERROR))
            }
        }
    }
}