package com.example.ptnet

import android.app.AlertDialog
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.postDelayed
import com.example.ptnet.Utils.getRunningVpn
import com.example.ptnet.Utils.now
import com.example.ptnet.interfaces.CaptureStartListener
import com.example.ptnet.interfaces.ConnectionsListener
import com.example.ptnet.models.AppDescriptor
import com.example.ptnet.models.AppStats
import com.example.ptnet.models.CaptureSettings
import com.example.ptnet.models.ConnectionDescriptor
import com.example.ptnet.models.ConnectionUpdate
import com.example.ptnet.models.Prefs
import com.example.ptnet.services.CaptureHelper
import com.example.ptnet.services.CaptureService
import com.example.ptnet.services.VpnReconnectService
import com.example.ptnet.ui.theme.PtnetTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class MainActivity : ComponentActivity(), ConnectionsListener {
    private val TAG = "Main"
    private lateinit var settings: CaptureSettings
    private lateinit var helper: CaptureHelper
    private var mWasStarted = false
    private var mHandler: Handler? = null
    private var captureService: CaptureService? = null
    private var mPrefs: SharedPreferences? = null
    private var stopRefreshFlag: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefs = Prefs.defaultSetting(this)

        helper = CaptureHelper(this, true)
        helper.setListener(object : CaptureStartListener {
            override fun onCaptureStartResult(success: Boolean) {
                if (!success) {
                    Log.w(TAG, "Capture start failed")
                    appStateReady()
                }
            }
        })


        CaptureService().observeStatus(this) { serviceStatus ->
            if (serviceStatus != null) {
                Log.d(TAG, "Service status: " + serviceStatus.name)
            }
            if (serviceStatus == CaptureService.ServiceStatus.STARTED) {
                Log.d(TAG, "Service Start")
                mWasStarted = true
            } else if (mWasStarted) { /* STARTED -> STOPPED */
                // The service may still be active (on premature native termination)
                if (captureService!!.isServiceActive()) captureService!!.stopService()

                appStateReady()
                mWasStarted = false
            } else  /* STOPPED -> STOPPED */ appStateReady()
        }

        mHandler = Handler(Looper.getMainLooper())

        setContent {
            PtnetTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var runable by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Greeting("Android")
                        Spacer(modifier = Modifier.height(20.dp))
                        CaptureButton(runable = runable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (!runable) {
                                    Log.d(TAG, "Start Capture")
                                    stopRefreshFlag = false // Reset the stop flag
                                    mHandler?.postDelayed({
                                        Thread {
                                            startCapture()
                                            captureService = CaptureService.getInstance()
                                            doRefreshApps()
                                        }.start()
                                    }, 1000)
                                } else {
                                    Log.d(TAG, "Stop Capture")
                                    captureService!!.stopService()
                                    stopRefreshFlag = true
                                    mHandler?.removeCallbacksAndMessages(null)
                                }

                                runable = !runable
                            } else {
                                Log.d(TAG, "Check SDK version: Require 30")
                            }
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Finish init")
    }

    fun appStateReady() {
//        mState = AppState.ready
//        notifyAppState()
//        if (mPcapLoadDialog != null) checkLoadedPcap()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startCapture() {
        if (VpnReconnectService().isAvailable()) VpnReconnectService().stopService()
        if (showRemoteServerAlert()) return
//        if (Prefs.getTlsDecryptionEnabled(mPrefs!!) && needsSetup(this)) {
//            val intent = Intent(this, MitmSetupWizard::class.java)
//            startActivity(intent)
//            return
//        }

        if (!Prefs.isRootCaptureEnabled(mPrefs!!) && getRunningVpn(this) != null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.active_vpn_detected)
                .setMessage(R.string.disconnect_vpn_confirm)
                .setPositiveButton(R.string.ok) { dialog, whichButton -> doStartCaptureService(null) }
                .setNegativeButton(R.string.cancel_action) { dialog, whichButton -> }
                .show()
        } else doStartCaptureService(null)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun doStartCaptureService(input_pcap_path: String?) {
        settings = CaptureSettings(this, mPrefs!!)
        settings.input_pcap_path = input_pcap_path
        helper.startCapture(settings)
    }


    val mapConnections = HashMap<String, ConnectionDescriptor>()
    var connReg: ConnectionsRegister? = null
    var appsStats: List<AppStats>? = null
    var appsResolver: AppsResolver? = null
    val iconDir: MutableMap<Int, Drawable?> = mutableMapOf()

    // NOTE: do not use synchronized as it could cause a deadlock with the ConnectionsRegister lock
    private fun doRefreshApps() {
        // Get Connection Register
        connReg = captureService?.requireConnsRegister()

        val pmList = processAppsList()
        pmList.forEach { pm ->
            iconDir[pm.getUid()] = pm.getIcon()
        } // -> icon directory

        // Listen event
        connReg!!.addListener(this)



        // Will missing some case on load
        while (!stopRefreshFlag) {
            mHandler?.postDelayed({
                appsResolver = connReg?.getAppResolver()
                appsStats = connReg?.getAppsStats()

                appsStats?.forEach { app ->
                    val appDescriptor = appsResolver?.getAppByUid(app.getUid(), 0)
                    appDescriptor?.mIcon = iconDir[app.getUid()]
                    val appConnections =
                        mapConnections.keys.filter { it.contains("[${app.getUid()}]") }

                    val tmpConnections = HashMap<String, ConnectionDescriptor>()
                    appConnections.forEach { ac -> tmpConnections[ac] = mapConnections[ac]!! }
                    val tmpConnectionsString =
                        tmpConnections.entries.joinToString(separator = "\n ") { (key, value) ->
                            "Key: $key"
//                                     + "\nValue: $value"
                        }
                    Log.i("$TAG 10s check", "$appDescriptor\n $tmpConnectionsString")
                }
            }, 10000)

            Thread.sleep(10000)
        }
    }

    private val TERMUX_PACKAGE = "com.termux"
    private fun processAppsList(): List<AppDescriptor> {
        val pm = applicationContext.packageManager;
        val packs = Utils.getInstalledPackages(pm, 0)
        val app_package: String = applicationContext.packageName

        val papps = ArrayList<AppDescriptor>()
        val uid_to_pos: ArrayMap<Int, Int> = ArrayMap()
        val tstart: Long = Utils.now()
        var termuxPkgInfo: PackageInfo? = null

        // NOTE: a single uid can correspond to multiple packages, only take the first package found.
        // The VPNService in android works with UID, so this choice is not restrictive.
        // NOTE: a single uid can correspond to multiple packages, only take the first package found.
        // The VPNService in android works with UID, so this choice is not restrictive.
        for (i in packs.indices) {
            val p = packs[i]
            val package_name = p.applicationInfo.packageName
            if (package_name == TERMUX_PACKAGE) termuxPkgInfo = p
            if (!uid_to_pos.containsKey(p.applicationInfo.uid) && package_name != app_package) {
                val uid = p.applicationInfo.uid
                val app = AppDescriptor(pm, p)
                uid_to_pos[uid] = papps.size
                papps.add(app)
            }
        }

        if (termuxPkgInfo != null) {
            // termux packages share the same UID. Use the main package if available. See #253
            val uid = termuxPkgInfo.applicationInfo.uid
            val pos: Int = uid_to_pos[uid]!!
            papps.removeAt(pos)
            papps.add(AppDescriptor(pm, termuxPkgInfo))
        }

        papps.sort()

        Log.d(TAG, packs.size.toString() + " apps loaded in " + (now() - tstart) + " seconds")
        return papps
    }

    // see also CaptureCtrl.checkRemoteServerNotAllowed
    private fun showRemoteServerAlert(): Boolean {
        val prefs = mPrefs!!

        if (prefs.getBoolean(
                Prefs.PREF_REMOTE_COLLECTOR_ACK,
                false
            )
        ) return false // already acknowledged

        if (Prefs.getDumpMode(prefs) === Prefs.DumpMode.UDP_EXPORTER && !Utils.isLocalNetworkAddress(
                Prefs.getCollectorIp(prefs)
            ) || Prefs.getSocks5Enabled(prefs) && !Utils.isLocalNetworkAddress(
                Prefs.getSocks5ProxyHost(
                    prefs
                )
            )
        ) {
            Log.i(TAG, "Showing possible scan notice")
            val dialog: AlertDialog = AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.remote_collector_notice)
                .setPositiveButton(R.string.ok) { d, whichButton ->
                    prefs.edit().putBoolean(Prefs.PREF_REMOTE_COLLECTOR_ACK, true).apply()
                }
                .show()

            mPrefs = prefs
            dialog.setCanceledOnTouchOutside(false)
            return true
        }
        return false
    }

    override fun connectionsChanges(numOfConnection: Int) {
        mHandler?.post { Log.i(TAG, "New connections size: $numOfConnection") }
    }

    override fun connectionsAdded(start: Int, descriptorArray: Array<ConnectionDescriptor?>?) {
        mHandler?.post { add(start, descriptorArray) }
    }

    override fun connectionsRemoved(start: Int, descriptorArray: Array<ConnectionDescriptor?>?) {
        mHandler?.post {
            Log.i(
                TAG,
                "Remove " + descriptorArray?.size + " connections at " + start
            )

            descriptorArray?.forEach { cd ->
                val c = cd!!
                mapConnections.remove(descriptionString(c))
            }
        }
    }

    override fun connectionsUpdated(positions: IntArray?) {
        mHandler?.post {
            update(positions)
        }
    }

    fun add(start: Int, descriptorArray: Array<ConnectionDescriptor?>?) {
        descriptorArray?.forEach { cd ->
            val c = cd!!
            mapConnections[descriptionString(c)] = c
        }

        Log.i(
            TAG,
            "Add ${descriptorArray?.size} connection(s). Total connections: ${mapConnections.size}"
        )
    }

    fun update(positions: IntArray?) {
        val reg: ConnectionsRegister = captureService?.requireConnsRegister()!!
        var first_removed_pos = -1
        var num_just_removed = 0
        positions?.sort()
        for (reg_pos in positions!!) {
            val conn = reg.getConn(reg_pos)
            if (conn != null) {
                Log.i(TAG, "Connection updated: \n $conn")
                mapConnections[descriptionString(conn)] = conn
            }
        }
    }

    private fun descriptionString(c: ConnectionDescriptor): String {
        return "[${c.uid}][${c.l7proto}][${c.srcPort}:${c.srcIp}][${c.dstPort}:${c.dstIp}]"
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun CaptureButton(runable: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(72.dp)
            .width(200.dp)
            .padding(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = if (!runable) "Execute" else "Stop",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PtnetTheme {
        Greeting("Android")
    }
}