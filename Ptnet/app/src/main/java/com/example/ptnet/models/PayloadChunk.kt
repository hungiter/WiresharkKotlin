package com.example.ptnet.models

import java.io.Serializable


class PayloadChunk : Serializable {
    lateinit var payload: ByteArray
    lateinit var type: ChunkType
    var isSent = false
    var timestamp: Long = 0

    lateinit var contentType: String
    lateinit var path: String

    constructor()

    constructor(payload: ByteArray, type: ChunkType, isSent: Boolean, timestamp: Long) {
        this.payload = payload
        this.type = type
        this.isSent = isSent
        this.timestamp = timestamp
    }

    enum class ChunkType : Serializable {
        RAW,
        HTTP,
        WEBSOCKET
    }

    fun subchunk(start: Int, size: Int): PayloadChunk {
        val subarr = ByteArray(size)
        System.arraycopy(payload, start, subarr, 0, size)
        return PayloadChunk(subarr, type, isSent, timestamp)
    }

    fun withPayload(the_payload: ByteArray?): PayloadChunk {
        return PayloadChunk(the_payload!!, type, isSent, timestamp)
    }

    override fun toString(): String {
        return "PayloadChunk(payload=${payload.contentToString()}, type=$type, isSent=$isSent, timestamp=$timestamp, contentType='$contentType', path='$path')"
    }
}
