package org.secuso.privacyfriendlybackup.ui.encryption

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.work.*
import org.openintents.openpgp.util.OpenPgpApi
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_KEY
import org.secuso.privacyfriendlybackup.preference.PreferenceKeys.PREF_ENCRYPTION_PASSPHRASE
import org.secuso.privacyfriendlybackup.worker.EncryptionWorker
import org.secuso.privacyfriendlybackup.worker.datakeys.*

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
        dataId = intent.getLongExtra(DATA_ID, -1)
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
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val edit = pref.edit()

            val newPassphrase = data?.getStringExtra(OpenPgpApi.EXTRA_PASSPHRASE)
            if(!newPassphrase.isNullOrEmpty() && passphrase != newPassphrase) {
                passphrase = newPassphrase
                edit.putString(PREF_ENCRYPTION_PASSPHRASE, newPassphrase)
            }

            val newSignKey = data?.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, -1L)
            if(newSignKey != null && newSignKey != -1L) {
                signingKey = newSignKey
                edit.putLong(PREF_ENCRYPTION_KEY, newSignKey)
            }

            val newSelectedKeyIds = data?.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS)
            if(newSelectedKeyIds != null && newSelectedKeyIds.isEmpty()) {
                selectedKeyIds = newSelectedKeyIds
                // TODO: this might have to be changed if backup sharing is implemented
            }

            edit.apply()

            // after the answer was received, there is no need to display this translucent activity
            finish()

/*
            var encryptionWorker : OneTimeWorkRequest?

            when(requestCode) {
                EncryptionWorker.REQUEST_CODE_DECRYPT_AND_VERIFY,
                EncryptionWorker.REQUEST_CODE_SIGN_AND_ENCRYPT -> {
                    encryptionWorker = OneTimeWorkRequestBuilder<EncryptionWorker>().setInputData(
                        workDataOf(
                            DATA_ID to dataId,
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

            WorkManager.getInstance(this).beginUniqueWork("$callingPackageName($dataId)", ExistingWorkPolicy.REPLACE, encryptionWorker!!).enqueue()
*/
        }
    }

}