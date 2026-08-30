package com.pulse.checkin.data.update

import kotlinx.serialization.Serializable

@Serializable
data class VersionManifest(
    val versionCode: Int,
    val versionName: String,
    val notes: String = "",
    val url: String = "",
)
