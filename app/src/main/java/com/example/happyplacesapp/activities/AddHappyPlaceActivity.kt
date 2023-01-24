package com.example.happyplacesapp.activities


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.happyplacesapp.R
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.Constants
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*




class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityAddHappyPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage : Uri? = null
    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0

    private lateinit var mProgressDialog: Dialog

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    companion object {
        const val CAMERA_PERMISSION_CODE = 1
        const val CAMERA_REQUEST_CODE = 2
        const val PLACE_AUTOCOMPLETE_REQUESTCODE = 3
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setupToolbar()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        dateSetListener = DatePickerDialog.OnDateSetListener{
                _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        if(intent.hasExtra(Constants.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getParcelableExtra(Constants.EXTRA_PLACE_DETAILS) as HappyPlaceModel?
        }

        if(mHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place"
            binding?.btnSave?.text = "Save"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            binding?.ivImageSrc?.setImageURI(Uri.parse(mHappyPlaceDetails!!.image))
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude


            saveImageToInternalStorage = (Uri.parse(mHappyPlaceDetails!!.image))
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddimage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)

        if(!Places.isInitialized()){
            Places.initialize(this@AddHappyPlaceActivity, resources.getString(R.string.google_maps_key))
        }

    }

    private fun setupToolbar() {
        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onClick(p0: View?) {
        when(p0!!.id){
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.tv_addimage -> {
                Log.e("tv_addimage", "WORKED")
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select Photo from Gallery", "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                    _, which -> when(which) {
                        0 -> {choosePhotoFromGallery()}
                        1 -> {capturePhotoFromCamera()}
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save ->{
               if (binding?.etTitle?.text.isNullOrEmpty() || saveImageToInternalStorage == null) {
                    Toast.makeText(this, "You need at least to enter a title and image.", Toast.LENGTH_SHORT).show()
               } else {
                   val handler = DatabaseHandler(this)
                   val model = HappyPlaceModel(
                       if(mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                       binding?.etTitle?.text.toString(),
                       saveImageToInternalStorage.toString(),
                       binding?.etDescription?.text.toString(),
                       binding?.etDate?.text.toString(),
                       binding?.etLocation?.text.toString(),
                       mLatitude,
                       mLongitude
                   )


                   if (mHappyPlaceDetails == null) {
                       val handlerResult = handler.addHappyPlace(model)
                       if(handlerResult > 0){
                           Toast.makeText(this, "Happy place added!", Toast.LENGTH_SHORT).show()
                           setResult(Activity.RESULT_OK)
                           finish()
                       }
                   } else {
                       val handlerResult = handler.updateHappyPlace(model)
                       if(handlerResult > 0){
                           Toast.makeText(this, "Happy place saved!", Toast.LENGTH_SHORT).show()
                           setResult(Activity.RESULT_OK)
                           finish()
                       }
                   }

               }
            }
            R.id.et_location ->{
                try{
                    val fields = listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )

                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(this@AddHappyPlaceActivity)

                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUESTCODE)

                }catch(e:Exception){
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location ->{

                if(!isLocationEnabled()){
                    Toast.makeText(this, "Your location provider is turned off. Please turn your GPS on.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else{
                    Dexter.withActivity(this).withPermissions(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object : MultiplePermissionsListener{

                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                showProgressDialog()
                                requestLocationData()
                            }

                            if (report.isAnyPermissionPermanentlyDenied) {
                                Toast.makeText(this@AddHappyPlaceActivity, "You have denied location permission.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?
                        ) {
                            showRationalDialogPermissions()
                        }

                    }).onSameThread().check()
                }

            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun capturePhotoFromCamera() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else{
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }
        } else{
            showRationalDialogPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                Constants.CAMERA -> {
                    val thumbNail: Bitmap = data!!.extras!!.get("data") as Bitmap
                    binding?.ivImageSrc?.setImageBitmap(thumbNail)
                    saveImageToInternalStorage = saveImageToInternalStorage(thumbNail)
                }
                Constants.GALLERY ->{
                    if(data != null){
                        val contentURI = data.data
                        try {
                            val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)
                            saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)

                            Log.e("Saved Image", "path: $saveImageToInternalStorage")

                            binding?.ivImageSrc?.setImageBitmap(selectedImageBitmap)

                        } catch (e: IOException){
                            e.printStackTrace()
                            Toast.makeText(this, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Constants.PLACE_AUTOCOMPLETE_REQUESTCODE ->{
                    val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                    binding?.etLocation?.setText(place.address)
                    mLatitude = place.latLng!!.latitude
                    mLongitude = place.latLng!!.longitude

                }
            }

        }
    }

    private fun choosePhotoFromGallery() {
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ).withListener(object: MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report?.areAllPermissionsGranted() == true){
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, Constants.GALLERY)
                    Toast.makeText(this@AddHappyPlaceActivity,"Permissions to READ/WRITE storage granted.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationalDialogPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogPermissions() {
        AlertDialog.Builder(this).setMessage(
            "The permissions for this features have been denied. The permissions can be enabled in Applications Settings.")
            .setPositiveButton("GO TO SETTING"){
                _,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch(e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CANCEL"){
                dialog, _ -> dialog.dismiss()
            }
            .show()
  }

    private fun updateDateInView(){
        val myFormat = "dd/MM/yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(Constants.IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try{
            val stream : OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException){
            e.printStackTrace()
        }

        return  Uri.parse(file.absolutePath)

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest.create().apply {
            interval = 0
            numUpdates = 1
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location? = locationResult.lastLocation
            if (mLastLocation != null) {
                mLatitude = mLastLocation.latitude
            }
            Log.e("Current Latitude", "$mLatitude")
            if (mLastLocation != null) {
                mLongitude = mLastLocation.longitude
            }
            Log.e("Current Longitude", "$mLongitude")

            getAddressFromLatLng(mLatitude, mLongitude)
        }
    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double){

        val geocoder = Geocoder(this, Locale.getDefault())

        lifecycleScope.launch {
            val addressList: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addressList != null && addressList.isNotEmpty()) {
                val address: Address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                sb.deleteCharAt(sb.length - 1)
                binding?.etLocation?.setText(sb.toString())
            } else {
                Toast.makeText(
                    this@AddHappyPlaceActivity,
                    "Failed to load location",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        hideProgressDialog()
    }

    private fun showProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_progress)
        mProgressDialog.show()
    }

    private fun hideProgressDialog(){
        mProgressDialog.dismiss()
    }

}