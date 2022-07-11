package io.ffem.lite.ui

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.aflak.arduino.Arduino
import me.aflak.arduino.ArduinoListener

class SensorFragment : SensorFragmentBase(), ArduinoListener {

    private var sensor: Arduino? = null
    private var usbDevice: UsbDevice? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensor = Arduino(context)
        sensor!!.setArduinoListener(this)
    }

    override fun onArduinoAttached(device: UsbDevice?) {
        usbDevice = device
        enableEmptyCalibration()
    }

    override fun onArduinoMessage(bytes: ByteArray) {
        handleMessage(bytes)
    }

    override fun send(s: String) {
        sensor!!.send(s.toByteArray())
    }

    override fun startSensor() {
        sensor!!.close()
        sensor!!.open(usbDevice)
    }

    override fun onArduinoOpened() {
        display("Connected")
        currentPulseWidth = ""
        startTimer()
    }

    override fun onArduinoDetached() {
        resetInterface()
        sensor!!.setArduinoListener(this)
    }

    override fun onUsbPermissionDenied() {
        display("Permission denied. Trying again...")
        MainScope().launch {
            delay(3000)
            sensor!!.reopen()
        }
    }

    override fun onDestroy() {
        try {
            sensor!!.unsetArduinoListener()
            sensor!!.close()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}