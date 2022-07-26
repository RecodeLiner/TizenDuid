package com.wearsoft.tizenduid

import java.io.IOException

internal class SdbStreamClosed(localId: Int) : IOException(String.format("SDB stream is closed for localId: %x", localId))