package com.example.happyplacesapp


import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.happyplacesapp.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplacesapp.utils.Constants
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlace : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityAddHappyPlaceBinding? = null
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    companion object {
        const val CAMERA_PERMISSION_CODE = 1
        const val CAMERA_REQUEST_CODE = 2
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        dateSetListener = DatePickerDialog.OnDateSetListener{
                _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddimage?.setOnClickListener(this)


    }

    override fun onClick(p0: View?) {
        when(p0!!.id){
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlace,
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
        }
    }

    private fun capturePhotoFromCamera() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent,CAMERA_REQUEST_CODE)
        } else{
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE)
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
                startActivityForResult(intent,CAMERA_REQUEST_CODE)
            }
        } else{
            showRationalDialogForPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == Constants.CAMERA){
                val thumbNail: Bitmap = data!!.extras!!.get("data") as Bitmap
                binding?.ivImageSrc?.setImageBitmap(thumbNail)
            } else if (requestCode == Constants.GALLERY) {
                if(data != null){
                    val contentURI = data.data
                    try {
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)
                        val savedImage = saveImageToInternalStorage(selectedImageBitmap)

                        Log.e("Saved Image", "path: $savedImage")

                        binding?.ivImageSrc?.setImageBitmap(selectedImageBitmap)

                    } catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show()
                    }
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
                    Toast.makeText(this@AddHappyPlace,"Permissions to READ/WRITE storage granted.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions() {
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

}