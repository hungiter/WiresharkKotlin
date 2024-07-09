package com.example.ptnet

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import android.util.SparseArray
import androidx.lifecycle.MutableLiveData
import com.example.ptnet.Utils.safeClose
import com.example.ptnet.Utils.showToastLong
import com.example.ptnet.interfaces.ConnectionsListener
import com.example.ptnet.models.CaptureSettings
import com.example.ptnet.models.ConnectionDescriptor
import com.example.ptnet.models.MitmAPI.MitmConfig
import com.example.ptnet.models.MitmAddon
import com.example.ptnet.models.MitmListener
import com.example.ptnet.models.Prefs
import com.example.ptnet.services.CaptureService
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException


class MitmReceiver : Runnable, ConnectionsListener, MitmListener {
    private val TAG = "MitmReceiver"
    val TLS_DECRYPTION_PROXY_PORT = 7780
    private var mThread: Thread? = null
    private lateinit var mReg: ConnectionsRegister
    private lateinit var mContext: Context
    private lateinit var mAddon: MitmAddon
    private lateinit var mConfig: MitmConfig
    private var mPcapngFormat = false
    private var proxyStatus: MutableLiveData<Status> = MutableLiveData<Status>(Status.NOT_STARTED)
    private var mSocketFd: ParcelFileDescriptor? = null
    private lateinit var mKeylog: BufferedOutputStream

    // Shared state
    private val mPortToConnId: LruCache<Int, Int> = LruCache(64)
    private val mPendingMessages: SparseArray<ArrayList<PendingMessage>> =
        SparseArray<ArrayList<PendingMessage>>()


    private enum class MsgType {
        UNKNOWN, RUNNING, TLS_ERROR, HTTP_ERROR, HTTP_REQUEST,
        HTTP_REPLY, TCP_CLIENT_MSG, TCP_SERVER_MSG, TCP_ERROR, WEBSOCKET_CLIENT_MSG,
        WEBSOCKET_SERVER_MSG, DATA_TRUNCATED, MASTER_SECRET, LOG, JS_INJECTED
    }

    private class PendingMessage {
        lateinit var type: MsgType
        lateinit var msg: ByteArray
        var port: Int = 0
        var pendingSince: Long = 0
        var `when`: Long = 0

        constructor(type: MsgType, msg: ByteArray, port: Int, `when`: Long) {
            this.type = type
            this.msg = msg
            this.port = port
            this.pendingSince = SystemClock.elapsedRealtime()
            this.`when` = `when`
        }
    }


    enum class Status {
        NOT_STARTED,
        STARTING,
        START_ERROR,
        RUNNING
    }

    constructor()

    constructor(ctx: Context, settings: CaptureSettings, proxyAuth: String?) {
        mContext = ctx
        mReg = CaptureService().requireConnsRegister()
        mAddon = MitmAddon(mContext, this)
        mPcapngFormat = settings.pcapng_format
        mConfig = MitmConfig()
        mConfig.proxyPort = TLS_DECRYPTION_PROXY_PORT
        mConfig.proxyAuth = proxyAuth
        mConfig.dumpMasterSecrets = CaptureService().getDumpMode() !== Prefs.DumpMode.NONE
        mConfig.additionalOptions = settings.mitmproxy_opts
        mConfig.shortPayload = !settings.full_payload

        /* upstream certificate verification is disabled because the app does not provide a way to let the user
           accept a given cert. Moreover, it provides a workaround for a bug with HTTPS proxies described in
           https://github.com/mitmproxy/mitmproxy/issues/5109 */
        mConfig.sslInsecure = true

        // root capture uses transparent mode (redirection via iptables)
        mConfig.transparentMode = settings.root_capture
        getKeylogFilePath(mContext).delete()
    }

    private fun getKeylogFilePath(ctx: Context): File {
        return File(ctx.cacheDir, "SSLKEYLOG.txt")
    }

    @Throws(IOException::class)
    fun start(): Boolean {
        Log.d(TAG, "starting")
        proxyStatus.postValue(Status.STARTING)
        if (!mAddon.connect(Context.BIND_IMPORTANT)) {
            showToastLong(mContext, R.string.mitm_start_failed)
            return false
        }
        mReg.addListener(this)
        return true
    }

    @Throws(IOException::class)
    fun stop() {
        Log.d(TAG, "stopping")
        mReg.removeListener(this)
        val fd = mSocketFd
        mSocketFd = null
        safeClose(fd) // possibly wake mThread

        // send explicit stop message, as the addon may not be waked when the fd is closed
        mAddon.stopProxy()

        // on some devices, calling close on the socket is not enough to stop the thread,
        // the service must be unbound
        mAddon.disconnect()
        while (mThread != null && mThread!!.isAlive) {
            try {
                Log.d(TAG, "Joining receiver thread...")
                mThread!!.join()
            } catch (ignored: InterruptedException) {
            }
        }
        mThread = null
        Log.d(TAG, "stop done")
    }


    override fun run() {
        TODO("Not yet implemented")
    }

    override fun connectionsChanges(numOfConnection: Int) {
        TODO("Not yet implemented")
    }

    override fun connectionsAdded(start: Int, descriptorArray: Array<ConnectionDescriptor?>?) {
        TODO("Not yet implemented")
    }

    override fun connectionsRemoved(start: Int, descriptorArray: Array<ConnectionDescriptor?>?) {
        TODO("Not yet implemented")
    }

    override fun connectionsUpdated(positions: IntArray?) {
        TODO("Not yet implemented")
    }

    override fun onMitmGetCaCertificateResult(ca_pem: String?) {
        TODO("Not yet implemented")
    }

    override fun onMitmServiceConnect() {
        TODO("Not yet implemented")
    }

    override fun onMitmServiceDisconnect() {
        TODO("Not yet implemented")
    }
}