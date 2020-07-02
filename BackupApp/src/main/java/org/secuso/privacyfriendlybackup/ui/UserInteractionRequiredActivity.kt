package org.secuso.privacyfriendlybackup.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import org.openintents.openpgp.util.OpenPgpApi
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_CALLING_PACKAGE_NAME
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_ENCRYPT
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_ID_TO_WORK_WITH
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_KEY_ID
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_OPENPGP_PROVIDER
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_PASSPHRASE
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker.Companion.DATA_SIGNING_KEY_ID

class UserInteractionRequiredActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_CODE = "EXTRA_REQUEST_CODE"
        const val TAG = "PFABackup"
    }

    var pi: PendingIntent? = null
    var requestCode : Int = -1
    var dataId : Long = -1
    var cryptoProviderPackage : String? = null
    var callingPackageName : String? = null
    var encrypt : Boolean = true
    var selectedKeyIds : LongArray? = null
    var signingKey : Long = -1L
    var passphrase : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "is this being executed?")

        pi = intent.getParcelableExtra(OpenPgpApi.RESULT_INTENT)
        requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
        dataId = intent.getLongExtra(DATA_ID_TO_WORK_WITH, -1)
        cryptoProviderPackage = intent.getStringExtra(DATA_OPENPGP_PROVIDER)
        encrypt = intent.getBooleanExtra(DATA_ENCRYPT, true)
        selectedKeyIds = intent.getLongArrayExtra(DATA_KEY_ID)
        passphrase = intent.getStringExtra(DATA_PASSPHRASE)
        callingPackageName = intent.getStringExtra(DATA_CALLING_PACKAGE_NAME)
        signingKey = intent.getLongExtra(DATA_SIGNING_KEY_ID, -1L)

        if(pi == null || requestCode == -1) {
            finish()
            return
        }

        try {
            startIntentSenderFromChild(
                this, pi!!.intentSender,
                requestCode, null, 0, 0, 0
            )
        } catch (e : IntentSender.SendIntentException ) {
            Log.e(TAG, "SendIntentException", e);
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK) {
            val newPassphrase = data?.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE)
            if(!newPassphrase.isNullOrEmpty() && passphrase != newPassphrase) {
                passphrase = newPassphrase
            }

            val newSelectedKeyIds = data?.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS)
            if(newSelectedKeyIds != null && newSelectedKeyIds!!.isEmpty()) {
                selectedKeyIds = newSelectedKeyIds
            }

            val newSignKey = data?.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, -1L)
            if(newSignKey != null && newSignKey != -1L) {
                signingKey = newSignKey
            }

            var encryptionWorker : OneTimeWorkRequest?

            when(requestCode) {
                EncryptionWorker.REQUEST_CODE_DECRYPT_AND_VERIFY,
                EncryptionWorker.REQUEST_CODE_SIGN_AND_ENCRYPT -> {
                    encryptionWorker = OneTimeWorkRequestBuilder<EncryptionWorker>().setInputData(
                        workDataOf(
                            DATA_ID_TO_WORK_WITH to dataId,
                            DATA_OPENPGP_PROVIDER to cryptoProviderPackage,
                            DATA_ENCRYPT to encrypt,
                            DATA_KEY_ID to selectedKeyIds,
                            DATA_SIGNING_KEY_ID to signingKey,
                            DATA_PASSPHRASE to passphrase,
                            DATA_CALLING_PACKAGE_NAME to callingPackageName
                        )
                    ).build()
                }
                else -> {
                    finish()
                    return
                }
            }

            // save in preferences for future
            //PreferenceManager.getDefaultSharedPreferences(this)

            WorkManager.getInstance(this).beginUniqueWork("$callingPackageName($dataId)", ExistingWorkPolicy.REPLACE, encryptionWorker!!).enqueue()
        }
    }

}