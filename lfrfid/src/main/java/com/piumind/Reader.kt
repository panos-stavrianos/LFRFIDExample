package com.piumind

import android.util.Log
import com.android.lflibs.DeviceControl
import com.android.lflibs.serial_native
import kotlin.concurrent.thread

class Reader {
    private var reader: Thread = thread { }
    private lateinit var nativeDev: serial_native
    private lateinit var devCtrl: DeviceControl
    fun read(rfidListener: RFIDListener) {
        try {
            devCtrl = DeviceControl(DEVICE_PATH)
            nativeDev = serial_native()
            if (nativeDev.OpenComPort(SERIAL_PORT_PATH) < 0) {
                Log.e(TAG, "Cannot open port")
                return
            }
            startReading(rfidListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startReading(rfidListener: RFIDListener) {
        reader = thread {
            try {
                devCtrl.powerOnDevice()
                Thread.sleep(5)
                nativeDev.ClearBuffer()
                while (true) {
                    val buf = nativeDev.ReadPort(BUF_SIZE)
                    if (buf != null && buf.size > 2) {
                        val hexMsg = String(buf.copyOfRange(1, buf.size - 2))
                        if (hexMsg.matches("-?[0-9a-fA-F]+".toRegex())) {
                            Log.e(TAG, hexMsg)
                            rfidListener.onNewRFID(hexMsg)
                        } else
                            Log.e(TAG, "Wrong Data")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        try {
            reader.interrupt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            devCtrl.powerOffDevice()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            devCtrl.deviceClose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            nativeDev.CloseComPort()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface RFIDListener {
        fun onNewRFID(rfid: String)
    }

    companion object {
        private const val SERIAL_PORT_PATH = "/dev/ttyMT2"
        private const val DEVICE_PATH = "/sys/class/misc/mtgpio/pin"
        private const val BUF_SIZE = 64
        private const val TAG = "LFRFID Reader"
    }

}