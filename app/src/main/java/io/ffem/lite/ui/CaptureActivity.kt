package io.ffem.lite.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import io.ffem.lite.R
import io.ffem.lite.qrcode.QRCodeReaderView
import kotlinx.android.synthetic.main.activity_capture.*

class CaptureActivity : BaseActivity(), ActivityCompat.OnRequestPermissionsResultCallback,
    QRCodeReaderView.OnQRCodeReadListener {

    private var mainLayout: ViewGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_capture)

        mainLayout = findViewById(R.id.main_layout)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initColorCard()
        } else {
            requestCameraPermission()
        }
    }

    override fun onResume() {
        super.onResume()

        if (colorCardView != null) {
            colorCardView.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()

        if (colorCardView != null) {
            colorCardView.stopCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
            return
        }

        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initColorCard()
        } else {
            Snackbar.make(mainLayout!!, getString(R.string.camera_permission_required), Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onQRCodeRead(text: String, points: Array<PointF?>) {
        pointsOverlayView.setPoints(points)
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Snackbar.make(
                mainLayout!!, getString(R.string.camera_permission_required),
                Snackbar.LENGTH_INDEFINITE
            ).setAction("OK") {
                ActivityCompat.requestPermissions(
                    this@CaptureActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    MY_PERMISSION_REQUEST_CAMERA
                )
            }.show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA), MY_PERMISSION_REQUEST_CAMERA
            )
        }
    }

    private fun initColorCard() {
        colorCardView.setAutofocusInterval(2000L)
        colorCardView.setOnQRCodeReadListener(this)
        colorCardView.setBackCamera()
        colorCardView.startCamera()
    }

    companion object {
        private const val MY_PERMISSION_REQUEST_CAMERA = 0
    }
}