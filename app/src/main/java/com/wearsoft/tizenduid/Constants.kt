package com.wearsoft.tizenduid

internal object Constants {

    const val AUTH_TYPE_TOKEN = 1
    const val AUTH_TYPE_SIGNATURE = 2
    const val AUTH_TYPE_RSA_PUBLIC = 3

    const val CMD_AUTH = 0x48545541//1213486401
    const val CMD_CNXN = 0x4e584e43//1314410051
    const val CMD_OPEN = 0x4e45504f//1313165391
    const val CMD_OKAY = 0x59414b4f//1497451343
    const val CMD_CLSE = 0x45534c43//1163086915
    const val CMD_WRTE = 0x45545257//1163154007

    const val CONNECT_VERSION = 0x01000000
    const val CONNECT_MAXDATA = 1024*1024

    val CONNECT_PAYLOAD = "host::\u0000".toByteArray()
}