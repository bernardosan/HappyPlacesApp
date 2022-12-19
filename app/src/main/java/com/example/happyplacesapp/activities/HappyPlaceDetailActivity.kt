package com.example.happyplacesapp.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.happyplacesapp.databinding.ActivityHappyPlaceDetailBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.Constants

class HappyPlaceDetailActivity : AppCompatActivity() {

    private var binding: ActivityHappyPlaceDetailBinding? = null
    private var mHappyPlacesModel: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if(intent.hasExtra(Constants.EXTRA_PLACE_DETAILS)){
            mHappyPlacesModel = intent.getParcelableExtra(Constants.EXTRA_PLACE_DETAILS) as HappyPlaceModel?
        }

        if(mHappyPlacesModel != null){
            setupToolbar()
            binding?.ivPlaceImage?.setImageURI(Uri.parse(mHappyPlacesModel!!.image))
            binding?.tvDescription?.text = mHappyPlacesModel!!.description
            binding?.tvLocation?.text = mHappyPlacesModel!!.location
        }

        binding?.btnViewOnMap?.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra(Constants.EXTRA_PLACE_DETAILS, mHappyPlacesModel)
            startActivity(intent)
        }


    }

    private fun setupToolbar() {
        setSupportActionBar(binding?.toolbarHappyPlaceDetail)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = mHappyPlacesModel!!.title
        binding?.toolbarHappyPlaceDetail?.setNavigationOnClickListener {
            onBackPressed()
        }
    }
}