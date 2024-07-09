package com.example.ptnet.models

import org.jetbrains.annotations.Nullable


interface MitmListener {
    // NOTE: for fragments, this may be called when their context is null
    fun onMitmGetCaCertificateResult(@Nullable ca_pem: String?)
    fun onMitmServiceConnect()
    fun onMitmServiceDisconnect()
}