package io.ffem.lite.ui

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ffem.lite.R
import io.ffem.lite.common.RESULT_NO_DATA
import io.ffem.lite.common.TEST_INFO
import io.ffem.lite.data.CalibrationDatabase
import io.ffem.lite.databinding.ActivityCalibrationsBinding
import io.ffem.lite.model.Calibration
import io.ffem.lite.model.TestInfo
import io.ffem.lite.util.SnackbarUtils
import io.ffem.lite.util.ThemeUtils
import io.ffem.lite.util.isAppInLockTaskMode

class CalibrationsActivity : BaseActivity() {
    private lateinit var b: ActivityCalibrationsBinding
    private var testId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeUtils = ThemeUtils(this)
        setTheme(themeUtils.appTheme)
        super.onCreate(savedInstanceState)
        b = ActivityCalibrationsBinding.inflate(layoutInflater)
        val view = b.root
        setContentView(view)

        val testInfo: TestInfo = intent.getParcelableExtra(TEST_INFO)!!
        testId = testInfo.uuid

        b.calibrationsList.setHasFixedSize(true)
        b.calibrationsList.layoutManager = LinearLayoutManager(this)
        b.calibrationsList.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        val db = CalibrationDatabase.getDatabase(this)
        try {
            val calibrationDetails = db.calibrationDao().getAllCalibrations(testId)
            if (calibrationDetails != null && calibrationDetails.isNotEmpty()) {
                with(b.calibrationsList) {
                    adapter = CalibrationsAdapter(
                        this@CalibrationsActivity::onCalibrationClick,
                        this@CalibrationsActivity::onDeleteResultClick,
                        calibrationDetails
                    )
                }
            } else {
                setResult(RESULT_NO_DATA)
                finish()
            }
        } finally {
            db.close()
        }

        title = getString(R.string.load_calibration)
    }

    private fun onDeleteResultClick(calibrationId: Long): Boolean {
        val alert = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_calibration)
            .setMessage(R.string.delete_calibration_warning)
            .setPositiveButton(R.string.delete) { _, _ -> deleteCalibration(calibrationId) }
            .setNegativeButton(R.string.cancel, null)
            .show()

        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        return true
    }

    private fun deleteCalibration(calibrationId: Long) {
        val db: CalibrationDatabase = CalibrationDatabase.getDatabase(this)
        try {
            val dao = db.calibrationDao()
            dao.deleteCalibrations(calibrationId)
            dao.deleteDetail(calibrationId)

            SnackbarUtils.showLongSnackbar(b.rootLayout, getString(R.string.calibration_deleted))

            val calibrationDetails = db.calibrationDao().getAllCalibrations(testId)
            if (calibrationDetails != null) {
                with(b.calibrationsList) {
                    adapter = CalibrationsAdapter(
                        this@CalibrationsActivity::onCalibrationClick,
                        this@CalibrationsActivity::onDeleteResultClick,
                        calibrationDetails
                    )
                }
            }
        } finally {
            db.close()
        }
        b.calibrationsList.adapter!!.notifyDataSetChanged()
    }

    private fun onCalibrationClick(calibrationId: Long) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.load_calibration)
            .setMessage(R.string.load_calibration_warning)
            .setPositiveButton(R.string.load) { _, _ -> loadCalibration(calibrationId) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadCalibration(calibrationId: Long) {
        val db: CalibrationDatabase = CalibrationDatabase.getDatabase(this)
        try {
            val dao = db.calibrationDao()
            val calibrationInfo = dao.getCalibrations(calibrationId)
            if (calibrationInfo != null) {
                val currentCalibrationId = dao.getCurrentCalibrationId(testId)
                dao.deleteCalibrations(currentCalibrationId)
                val newCalibrations = ArrayList<Calibration>()
                for (calibration in calibrationInfo.calibrations) {
                    val newCalibration = calibration.copy()
                    newCalibration.calibrationId = currentCalibrationId
                    newCalibrations.add(newCalibration)
                }

                val calibrationDetail = dao.getCalibrationDetail(testId)
                calibrationDetail!!.name = calibrationInfo.details.name
                calibrationDetail.desc = calibrationInfo.details.desc
                calibrationDetail.date = calibrationInfo.details.date
                dao.update(calibrationDetail)
                dao.insertAll(newCalibrations)
            }
        } finally {
            db.close()
        }

        Toast.makeText(this, R.string.calibration_loaded, Toast.LENGTH_LONG).show()

        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isAppInLockTaskMode(this)) {
                    showLockTaskEscapeMessage()
                } else {
                    finish()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}