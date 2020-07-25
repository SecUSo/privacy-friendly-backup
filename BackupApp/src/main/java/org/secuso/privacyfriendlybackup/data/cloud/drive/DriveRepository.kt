package org.secuso.privacyfriendlybackup.data.cloud.drive

import retrofit2.Retrofit

class DriveRepository {

    val driveService = Retrofit.Builder()
        .baseUrl("https://")

}