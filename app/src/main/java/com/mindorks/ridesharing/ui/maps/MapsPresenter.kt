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
        }
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
        Log.e("TAG", "error")
    }
}