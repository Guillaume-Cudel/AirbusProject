package services

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import java.io.File

interface PictureService {

    fun showAlert(activity: Activity)
}