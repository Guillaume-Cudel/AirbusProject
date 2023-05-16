package services

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.guillaume.airbusproject.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PictureServiceImpl: PictureService {

    override fun showAlert(activity: Activity) {
        val builder = AlertDialog.Builder(activity as Context)
        builder.setMessage("Camera permission was denied. Unable to take a picture.")
        builder.setPositiveButton("OK", null)
        val dialog = builder.create()
        dialog.show()
    }


}