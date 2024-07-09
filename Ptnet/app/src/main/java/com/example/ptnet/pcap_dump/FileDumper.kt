package com.example.ptnet.pcap_dump

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.ptnet.interfaces.PcapDumper
import com.example.ptnet.services.CaptureService
import java.io.IOException
import java.io.OutputStream


class FileDumper : PcapDumper {
    val TAG = "FileDumper"
    private var mContext: Context? = null
    private var mPcapUri: Uri? = null
    private var mSendHeader = false
    private var mOutputStream: OutputStream? = null

    constructor()

    constructor(ctx: Context, pcapUri: Uri) {
        mContext = ctx
        mPcapUri = pcapUri
        mSendHeader = true
    }

    @Throws(IOException::class)
    override fun startDumper() {
        Log.d(TAG, "PCAP URI: $mPcapUri")
        mOutputStream = mContext!!.contentResolver.openOutputStream(mPcapUri!!, "rwt")
    }

    @Throws(IOException::class)
    override fun stopDumper() {
        mOutputStream!!.close()
    }

    override fun getBpf(): String {
        return ""
    }

    @Throws(IOException::class)
    override fun dumpData(data: ByteArray?) {
        if (mSendHeader) {
            mSendHeader = false
            mOutputStream!!.write(CaptureService().getPcapHeader())
        }
        mOutputStream!!.write(data)
    }
}