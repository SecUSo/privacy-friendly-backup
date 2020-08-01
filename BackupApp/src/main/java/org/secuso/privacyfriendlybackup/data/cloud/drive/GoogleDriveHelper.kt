package org.secuso.privacyfriendlybackup.data.cloud.drive

import android.content.Context
import android.util.Log
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import org.secuso.privacyfriendlybackup.util.BackupDataUtil
import java.io.*
import java.util.*


object GoogleDriveHelper {
    private const val TAG = "PFA Drive"
    private const val APPLICATION_NAME = "Privacy Friendly Backup"
    private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"
    private val SCOPES = listOf(DriveScopes.DRIVE_APPDATA)
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"

    private var service : Drive? = null

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    @Throws(IOException::class)
    private fun getCredentials(context: Context, HTTP_TRANSPORT: NetHttpTransport): Credential {
        // Load client secrets.
        val `in` = GoogleDriveHelper::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
                ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
        val clientSecrets: GoogleClientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        // Build flow and trigger user authorization request.
        val flow: GoogleAuthorizationCodeFlow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(context.filesDir, TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val cred = flow.loadCredential(flow.clientId)
        if(cred != null) {
            return cred
        }

        //val receiver: LocalServerReceiver = LocalServerReceiver.Builder().setPort(8888).build()
        //return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        return Credential.Builder(null).build() // TODO: This is wrong - correct flow is still work to be done
    }

    private fun getService(context: Context) : Drive {
        var tempService = service
        if(tempService == null) {
            synchronized(this) {
                // Build a new authorized API client service.
                val HTTP_TRANSPORT = NetHttpTransport()
                tempService = Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(context, HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build()
                service = tempService
            }
        }
        return service!!
    }

    fun listFiles(context: Context) {
        val service = getService(context)

        val files: FileList = service.files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name)")
            //.setPageSize(20)
            .execute()
        for (file in files.files) {
            System.out.printf("Found file: %s (%s)\n", file.name, file.id)
        }
    }

    fun createFile(context: Context, dataFile: File, encrypted : Boolean = false) : String {
        val service = getService(context)

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = dataFile.name
            fileExtension = "backup"
            id = dataFile.name // use the name as id
            parents = Collections.singletonList("appDataFolder")
        }

        val type = if(encrypted) "application/blob" else "application/json"

        val mediaContent = FileContent(type, dataFile)
        val file = service.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
        return file.id
    }

    fun readFile(context: Context, fileId: String, outputStream: OutputStream) {
        getService(context).Files().get(fileId).executeAsInputStream()
    }

    fun readFile(context: Context, fileId: String) : InputStream {
        return getService(context).Files().get(fileId).executeAsInputStream()
    }

    fun deleteFile(context: Context, fileId: String) {
        getService(context).Files().delete(fileId).execute()
    }
}