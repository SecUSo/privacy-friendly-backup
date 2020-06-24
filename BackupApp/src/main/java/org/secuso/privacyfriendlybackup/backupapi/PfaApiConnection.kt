package org.secuso.privacyfriendlybackup.backupapi

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import org.secuso.privacyfriendlybackup.api.IPFAService
import org.secuso.privacyfriendlybackup.api.pfa.PfaApi
import org.secuso.privacyfriendlybackup.api.pfa.PfaApi.MSG_AUTHENTICATE
import org.secuso.privacyfriendlybackup.api.pfa.PfaApi.MessageCodes

/**
 * @author Christopher Beckmann
 */
class PfaApiConnection(
    private val mContext: Context,
    private val mPFAServiceName: String) {

    //private var mMessenger: Messenger? = null
    //private var mReplyMessenger: Messenger = Messenger(PFAHandler(mContext))
    private var mService : IPFAService? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            //mMessenger = null
            mService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            //mMessenger = Messenger(service)
            mService = IPFAService.Stub.asInterface(service)
        }
    }

    internal class PFAHandler(context: Context) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                REPLY_AUTHENTICATION_OK -> {
                    //msg.sendingUid
                    TODO()
                }
                REPLY_AUTHENTICATION_ERROR -> handleErrorCode(msg.arg1)
                else -> super.handleMessage(msg)
            }
        }

        private fun handleErrorCode(@MessageReplyErrorCodes code: Int) {
            when(code) {
                ERROR_AUTH_CERT_MISMATCH -> {
                    TODO()
                }
                ERROR_AUTH_APPLICATION_NOT_FOUND -> {
                    TODO()
                }
                else -> {
                    TODO()
                }
            }
        }
    }

    fun connect() {
        Intent(PfaApi.CONNECT_ACTION).also { intent ->
            intent.setPackage(mPFAServiceName)
            mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun disconnect() {
        mContext.unbindService(mConnection)
    }

    fun authenticate() {
        send(MSG_AUTHENTICATE)
        //send(MSG_SCHEDULE)
    }

    private fun send(@MessageCodes what : Int) {
        if(!isBound()) return

        val msg = Message.obtain(null, what, 0, 0).apply {
            replyTo = mReplyMessenger
        }

        try {
            mMessenger?.send(msg)
        } catch (e : RemoteException) {
            e.printStackTrace()
        }
    }

    fun isBound() : Boolean {
        return mService != null
    }

}