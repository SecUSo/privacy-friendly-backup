package org.secuso.privacyfriendlybackup.backupapi

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.secuso.privacyfriendlybackup.api.IPFAService
import org.secuso.privacyfriendlybackup.api.pfa.PfaApi
import org.secuso.privacyfriendlybackup.api.pfa.PfaApi.EXTRA_CONNECT_PACKAGE_NAME
import org.secuso.privacyfriendlybackup.api.pfa.PfaError

const val API_VERSION = 1

/**
 * @author Christopher Beckmann
 */
class PfaApiConnection(
    private val mContext: Context,
    private val mPFAServiceName: String,
    private val mPfaApiListener: IPfaApiListener? = null) {

    interface IPfaApiListener {
        fun onBound(service : IPFAService?)
        fun onError(error : PfaError)
        fun onSuccess()
    }

    private var mService : IPFAService? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = IPFAService.Stub.asInterface(service)
            mPfaApiListener?.onBound(mService)
        }
    }

    fun send(action : String) {
        if(!isBound()) {
            mPfaApiListener?.onError(PfaError(PfaError.PfaErrorCode.SERVICE_NOT_BOUND, "Service is not bound."))
        }
        val result : Intent = mService!!.send(Intent().apply {
            putExtra(PfaApi.EXTRA_API_VERSION, API_VERSION)
            setAction(action)
        })
        when(result.getIntExtra(PfaApi.RESULT_CODE, -1)) {
            PfaApi.RESULT_CODE_SUCCESS ->   mPfaApiListener?.onSuccess()
            PfaApi.RESULT_CODE_ERROR ->     mPfaApiListener?.onError(PfaError(PfaError.PfaErrorCode.GENERAL_ERROR, TODO()))
            else ->                         mPfaApiListener?.onError(PfaError(PfaError.PfaErrorCode.GENERAL_ERROR, "RESULT_CODE unknown."))
        }
    }

    fun connect() {
        if(mService != null) {
            Intent(PfaApi.PFA_CONNECT_ACTION).also { intent ->
                // this is the name of the PFA to connect to
                intent.setPackage(mPFAServiceName)
                // this allows for other backup applications - the PFA will callback to this app
                intent.putExtra(EXTRA_CONNECT_PACKAGE_NAME, mContext.packageName)
                mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            }
        } else {
            mPfaApiListener?.onBound(mService)
        }
    }

    fun disconnect() {
        mContext.unbindService(mConnection)
    }

    fun isBound() : Boolean {
        return mService != null
    }
}