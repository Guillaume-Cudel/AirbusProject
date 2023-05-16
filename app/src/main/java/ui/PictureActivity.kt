package ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.guillaume.airbusproject.databinding.ActivityPictureBinding
import services.PictureService
import services.PictureServiceImpl
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy


class PictureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureBinding
    private val  CAMERA_PERMISSION_CODE = 1000
    private val TAG = "CameraXApp"
    private var pictureFile: File? = null
    private var picturePath: String? = null
    private val pictureService: PictureService = PictureServiceImpl()
    private var imageCapture: ImageCapture? = null
    private lateinit var myBitmap: Bitmap
    private lateinit var paint: Paint
    private lateinit var extraCanvas : Canvas
    private var downX = 0f
    private var downY = 0f
    private var upX = 0f
    private var upY = 0f



    private val REQUIRED_PERMISSIONS = mutableListOf (
    Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        askPermission()

        //--
        //startCamera()
        // Request camera permissions
        if (allPermissionsGranted()) {
            takePhoto()
            //openCameraInterface()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, CAMERA_PERMISSION_CODE)
            pictureService.showAlert(this)
        }

    }

    //-------------
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                @SuppressLint("ClickableViewAccessibility")
                override fun
                        onCaptureSuccess(image: ImageProxy){
                    val msg = "Photo capture succeeded"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    val bitmap = image.convertImageProxyToBitmap()

                    image.close()

                    val mutableBitmap: Bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    binding.drawingPicture.setImageBitmap(mutableBitmap)


                    //drawView.setBitmap(mutableBitmap)
                    //drawView.setOnTouchListener { view, event ->
                    //    drawView.onTouchEvent(event)
                    //    true
                    //}

                    // Affiche l'image capturée ici

                    extraCanvas = Canvas(mutableBitmap)
                    val paint = Paint()
                    paint.color = Color.RED
                    paint.style = Paint.Style.FILL
                    binding.drawingPicture.setOnTouchListener(View.OnTouchListener{
                            view, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                downX = event.x
                                downY = event.y
                                extraCanvas.drawLine(upX, upY, downX, downY, paint)
                                binding.drawingPicture.invalidate()
                            }

                            MotionEvent.ACTION_UP -> {
                                upX = event.x
                                upY = event.y
                                extraCanvas.drawLine(downX, downY, upX, upY, paint)
                                binding.drawingPicture.invalidate()
                            }
                        }
                        return@OnTouchListener true




                    })
                    extraCanvas.drawCircle(50f, 50f, 20f, paint)
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            /*// Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }*/

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    fun ImageProxy.convertImageProxyToBitmap(): Bitmap {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    //-------------

    @SuppressLint("SuspiciousIndentation")
    private fun askPermission(){
        val permissionGranted = requestCameraPermission()
            if (permissionGranted) {
                //openCameraInterface()
                takePhoto()
            }
    }

    // Ask permission to use camera
    private fun requestCameraPermission(): Boolean {
        var permissionGranted = false
        val cameraPermissionNotGranted = ContextCompat.checkSelfPermission(
            this as Context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_DENIED
        if (cameraPermissionNotGranted) {
            //val permission = arrayOf(Manifest.permission.CAMERA)
            /*val permission = REQUIRED_PERMISSIONS.all {
                    ContextCompat.checkSelfPermission(
                        baseContext, it) == PackageManager.PERMISSION_GRANTED
            }*/
            // Display permission dialog
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, CAMERA_PERMISSION_CODE)

        } else {
            // Permission already granted
            permissionGranted = true
        }
        return permissionGranted
    }

    // Get the permission popup
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.size == 1 && grantResults[0] ==    PackageManager.PERMISSION_GRANTED){
                // Permission was granted
                //openCameraInterface()
                takePhoto()
            }
            else{
                // Permission was denied
                pictureService.showAlert(this)
            }
        }
    }

    private fun openCameraInterface() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pictureFile = createImageFile()
        val pictureUri = FileProvider.getUriForFile(this, "com.guillaume.airbusproject.fileprovider", pictureFile!!)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)
        pickPicture.launch(intent)
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("dd-MM-yyyy_HH-mm").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
        // Save a file: path for use with ACTION_VIEW intents
        picturePath = image.absolutePath
        return image
    }

    private val pickPicture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                myBitmap = BitmapFactory.decodeFile(pictureFile!!.absolutePath)

                binding.drawingPicture.setImageBitmap(myBitmap)
                //binding.pictureFirst.setImageBitmap(myBitmap)
                //bitmapIsOk = true

            }
        }

}