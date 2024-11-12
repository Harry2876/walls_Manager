package com.example.wallsmanager

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var currentWallpaperImageView: ImageView
    private lateinit var setNewWallpaperButton: Button
    private lateinit var setWallpaperButton: Button
    private var selectedImageBitmap: Bitmap? = null
    private val wallpaperManager: WallpaperManager by lazy { WallpaperManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentWallpaperImageView = findViewById(R.id.currentWallpaperImageView)
        setNewWallpaperButton = findViewById(R.id.setNewWallpaperButton)
        setWallpaperButton = findViewById(R.id.setWallpaperButton)

        // Show the current wallpaper in the ImageView at launch
        requestAndShowWallpaper()

        // Request permissions at launch
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestImagePermissions()
        } else {
            requestLegacyStoragePermissions()
        }

        setNewWallpaperButton.setOnClickListener {
            // Check permissions when clicking "Set New Wallpaper"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestImagePermissions()
            } else {
                requestLegacyStoragePermissions()
            }
        }

        // Set wallpaper button functionality
        setWallpaperButton.setOnClickListener {
            selectedImageBitmap?.let { bitmap ->
                showWallpaperOptions(bitmap)
            }
        }
    }

    // Function to show current wallpaper after permission check
    private fun requestAndShowWallpaper() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestImagePermissions()
        } else {
            requestLegacyStoragePermissions()
        }
    }

    private fun showCurrentWallpaper() {
        try {
            val currentWallpaperBitmap = wallpaperManager.drawable?.toBitmap()
            currentWallpaperImageView.setImageBitmap(currentWallpaperBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to load current wallpaper", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestImagePermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED -> {
                openPhotoPicker()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                Toast.makeText(this, "Permission needed to select images.", Toast.LENGTH_LONG).show()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_IMAGE_PERMISSION
                )
            }
        }
    }

    private fun requestLegacyStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_IMAGE_PERMISSION
            )
        } else {
            openGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    openPhotoPicker()
                } else {
                    openGallery()
                }
            } else {
                Toast.makeText(this, "Permission denied to read images.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPhotoPicker() {
        val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
        photoPickerLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let {
                showImagePreview(it)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let {
                showImagePreview(it)
            }
        }
    }

    private fun showImagePreview(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        selectedImageBitmap = BitmapFactory.decodeStream(inputStream)
        currentWallpaperImageView.setImageBitmap(selectedImageBitmap)
        setWallpaperButton.visibility = View.VISIBLE
    }

    private fun showWallpaperOptions(bitmap: Bitmap) {
        try {
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM) // Sets wallpaper for home screen
            Toast.makeText(this, "Wallpaper set successfully for home screen.", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting wallpaper.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val REQUEST_IMAGE_PERMISSION = 1001
    }
}