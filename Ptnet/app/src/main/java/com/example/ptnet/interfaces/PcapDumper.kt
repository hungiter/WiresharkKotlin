package com.example.ptnet.interfaces

import java.io.IOException

interface PcapDumper {
    @Throws(IOException::class)
    fun startDumper()

    @Throws(IOException::class)
    fun stopDumper()

    fun getBpf(): String?

    @Throws(IOException::class)
    fun dumpData(data: ByteArray?)
}