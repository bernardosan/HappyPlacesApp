package com.example.happyplacesapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toolbar
import com.example.happyplacesapp.R
import com.example.happyplacesapp.databinding.ActivityMapsBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.Constants
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback{

    private var binding: ActivityMapsBinding? = null

    private var mHappyPlaceModel: HappyPlaceModel? = null

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mapView = binding?.map!!

        if(intent.hasExtra(Constants.EXTRA_PLACE_DETAILS)){
            mHappyPlaceModel = intent.getParcelableExtra(Constants.EXTRA_PLACE_DETAILS) as HappyPlaceModel?
        }

        if(mHappyPlaceModel != null){
            setupToolbar()
            mapView.getMapAsync(this)
            mapView.onCreate(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }


    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onStart() {
        mapView.onStart()
        super.onStart()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding?.toolbarMaps)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = mHappyPlaceModel!!.title
        binding?.toolbarMaps?.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onMapReady(map: GoogleMap) {

        if(mHappyPlaceModel?.latitude != null && mHappyPlaceModel?.longitude != null) {
            val position = LatLng(mHappyPlaceModel!!.latitude, mHappyPlaceModel!!.longitude)
            map.addMarker(MarkerOptions().position(position).title((mHappyPlaceModel!!.location)))
            val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 15f)
            map.animateCamera(newLatLngZoom)
        }
    }
}